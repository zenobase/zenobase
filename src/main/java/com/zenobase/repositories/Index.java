package com.zenobase.repositories;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import com.zenobase.common.Callback;
import com.zenobase.json.DomainNode;
import com.zenobase.json.NodeList;
import com.zenobase.json.OptimisticLock;
import com.zenobase.json.Schema;
import com.zenobase.services.Cluster;
import com.zenobase.services.SearchOrder;
import io.sentry.ISpan;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpType;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.search.Hit;

public class Index {

	private final String indexName;
	private final OpenSearchClient client;
	private boolean disableRefresh;

	public Index(String indexName, OpenSearchClient client) {
		this.indexName = indexName;
		this.client = client;
	}

	public void disableRefresh(boolean disable) {
		this.disableRefresh = disable;
	}

	private Refresh refreshPolicy(boolean refresh) {
		return (refresh && !disableRefresh) ? Refresh.True : Refresh.False;
	}

	public String getIndexName() {
		return indexName;
	}

	public void create(int replicas) {
		create(1, replicas);
	}

	public void create(int shards, int replicas) {
		try {
			String autoExpandReplicas = replicas == Integer.MAX_VALUE ? "0-all" : "0-" + replicas;
			boolean acknowledged = client
				.indices()
				.create(c ->
					c.index(indexName).settings(s -> s.numberOfShards(shards).autoExpandReplicas(autoExpandReplicas))
				)
				.acknowledged();
			Preconditions.checkState(acknowledged, "Expected acknowledgement of index creation: %s", indexName);
			Preconditions.checkState(new Cluster(client).isReady(), "Expected at least one shard in cluster");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void putMapping(Schema schema) {
		try {
			String json = schema.toJson().toString();
			client
				.generic()
				.execute(
					org.opensearch.client.opensearch.generic.Requests.builder()
						.endpoint(indexName + "/_mapping")
						.method("PUT")
						.json(json)
						.build()
				)
				.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void store(String id, ObjectNode node, boolean refresh) {
		index(id, node, refresh);
	}

	public void store(String id, DomainNode node, boolean refresh) {
		index(id, node, OpType.Create, refresh);
	}

	public void store(DomainNode node, boolean refresh) {
		store(node.getId(), node, refresh);
	}

	public void store(List<? extends DomainNode> nodes, boolean refresh) {
		index(nodes, OpType.Create, refresh);
	}

	public void update(DomainNode node, boolean refresh) {
		update(node.getId(), node, refresh);
	}

	public void update(String id, DomainNode node, boolean refresh) {
		index(id, node, OpType.Index, refresh);
	}

	private void index(String id, ObjectNode node, boolean refresh) {
		IndexRequest.Builder<ObjectNode> builder = new IndexRequest.Builder<ObjectNode>()
			.index(indexName)
			.id(id)
			.document(stripMetadata(node))
			.opType(OpType.Create)
			.refresh(refreshPolicy(refresh));
		withSpan("index", () -> {
			IndexResponse response = client.index(builder.build());
			DomainNode.VERSION.setValue(node, response.version());
			return null;
		});
	}

	private void index(String id, DomainNode node, OpType operation, boolean refresh) {
		IndexRequest.Builder<ObjectNode> builder = new IndexRequest.Builder<ObjectNode>()
			.index(indexName)
			.id(id)
			.document(stripMetadata(node.toJson()))
			.opType(operation)
			.refresh(refreshPolicy(refresh));
		if (operation == OpType.Index) {
			OptimisticLock lock = Preconditions.checkNotNull(
				node.getOptimisticLock(),
				"Missing optimistic lock: %s",
				node.toJson()
			);
			builder.ifSeqNo(lock.seqNo());
			builder.ifPrimaryTerm(lock.primaryTerm());
		}
		withSpan("index", () -> {
			IndexResponse response = client.index(builder.build());
			node.setVersion(response.version());
			node.setOptimisticLock(new OptimisticLock(response.seqNo(), response.primaryTerm()));
			return null;
		});
	}

	private void index(List<? extends DomainNode> nodes, OpType operation, boolean refresh) {
		int BATCH_SIZE = 5000;
		for (int begin = 0; begin < nodes.size(); begin += BATCH_SIZE) {
			int end = Math.min(begin + BATCH_SIZE, nodes.size());
			indexBatch(nodes.subList(begin, end), operation, refresh);
		}
	}

	private void indexBatch(List<? extends DomainNode> nodes, OpType operation, boolean refresh) {
		List<BulkOperation> operations = new ArrayList<>(nodes.size());
		for (DomainNode node : nodes) {
			ObjectNode doc = stripMetadata(node.toJson());
			BulkOperation.Builder opBuilder = new BulkOperation.Builder();
			if (operation == OpType.Create) {
				opBuilder.create(c -> c.index(indexName).id(node.getId()).document(doc));
			} else {
				OptimisticLock lock = Preconditions.checkNotNull(
					node.getOptimisticLock(),
					"Missing optimistic lock: %s",
					node.toJson()
				);
				opBuilder.index(idx ->
					idx
						.index(indexName)
						.id(node.getId())
						.document(doc)
						.ifSeqNo(lock.seqNo())
						.ifPrimaryTerm(lock.primaryTerm())
				);
			}
			operations.add(opBuilder.build());
		}
		withSpan("bulk", () -> {
			List<BulkResponseItem> items = client
				.bulk(b -> b.operations(operations).refresh(refreshPolicy(refresh)))
				.items();
			String failureMessage = getFailureMessage(items);
			if (failureMessage != null) {
				List<String> failed = new ArrayList<>();
				for (int i = 0; i < items.size(); ++i) {
					if (items.get(i).error() == null) {
						failed.add(nodes.get(i).getId());
					}
				}
				if (!failed.isEmpty()) {
					delete(failed, refresh);
				}
				throw new RuntimeException("Couldn't store an item: " + failureMessage);
			}
			for (int i = 0; i < items.size(); ++i) {
				DomainNode node = nodes.get(i);
				node.setVersion(items.get(i).version());
				node.setOptimisticLock(new OptimisticLock(items.get(i).seqNo(), items.get(i).primaryTerm()));
			}
			return null;
		});
	}

	private static @Nullable String getFailureMessage(List<BulkResponseItem> items) {
		for (BulkResponseItem item : items) {
			if (item.error() != null) {
				return item.error().reason();
			}
		}
		return null;
	}

	private static ObjectNode stripMetadata(ObjectNode node) {
		ObjectNode copy = node.deepCopy();
		copy.remove(DomainNode.VERSION.getName());
		copy.remove(DomainNode.SEQ_NO_FIELD);
		copy.remove(DomainNode.PRIMARY_TERM_FIELD);
		return copy;
	}

	public boolean delete(String id, boolean refresh) {
		return withSpan("delete", () ->
			client
				.delete(d -> d.index(indexName).id(id).refresh(refreshPolicy(refresh)))
				.result()
				.jsonValue()
				.equals("deleted")
		);
	}

	public boolean delete(List<String> ids, boolean refresh) {
		List<BulkOperation> operations = new ArrayList<>();
		for (String id : ids) {
			operations.add(BulkOperation.of(op -> op.delete(d -> d.index(indexName).id(id))));
		}
		withSpan("bulk_delete", () -> {
			BulkResponse bulkResponse = client.bulk(b -> b.operations(operations).refresh(refreshPolicy(refresh)));
			String failureMessage = getFailureMessage(bulkResponse.items());
			if (failureMessage != null) {
				throw new RuntimeException("Couldn't delete an item: " + failureMessage);
			}
			return null;
		});
		return true;
	}

	public boolean exists() {
		try {
			return client
				.indices()
				.exists(e -> e.index(indexName))
				.value();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void refresh() {
		if (!disableRefresh) {
			try {
				client.indices().refresh(r -> r.index(indexName));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public NodeList find(Query query) {
		return find(SearchRequest.of(s -> s.index(indexName).query(query).version(true).seqNoPrimaryTerm(true)));
	}

	public NodeList find(SearchRequest request) {
		SearchResponse<ObjectNode> response = search(request);
		List<ObjectNode> nodes = new ArrayList<>(response.hits().hits().size());
		for (Hit<ObjectNode> hit : response.hits().hits()) {
			nodes.add(read(hit));
		}
		return new NodeList(nodes, response.hits().total().value());
	}

	public SearchResponse<ObjectNode> search(SearchRequest request) {
		return withSpan("search", () -> client.search(request, ObjectNode.class));
	}

	public void find(Query query, SearchOrder order, Callback<ObjectNode> callback, int pageSize) {
		find(
			() -> {
				var builder = new SearchRequest.Builder()
					.index(indexName)
					.query(query)
					.size(pageSize)
					.version(true)
					.seqNoPrimaryTerm(true);
				order.apply(builder);
				return builder;
			},
			callback
		);
	}

	public void find(Supplier<SearchRequest.Builder> requestBuilder, Callback<ObjectNode> callback) {
		List<FieldValue> searchAfter = null;
		while (true) {
			final List<FieldValue> currentSearchAfter = searchAfter;
			SearchRequest.Builder builder = requestBuilder.get();
			if (currentSearchAfter != null) {
				builder.searchAfter(currentSearchAfter);
			}
			SearchResponse<ObjectNode> response = withSpan("search", () ->
				client.search(builder.build(), ObjectNode.class)
			);
			var hits = response.hits().hits();
			if (hits.isEmpty()) {
				break;
			}
			for (Hit<ObjectNode> hit : hits) {
				callback.call(read(hit));
			}
			searchAfter = hits.getLast().sort();
		}
	}

	public @Nullable ObjectNode get(String id) {
		return withSpan("get", () -> {
			GetResponse<ObjectNode> response = client.get(g -> g.index(indexName).id(id), ObjectNode.class);
			if (!response.found()) return null;
			ObjectNode node = response.source();
			DomainNode.VERSION.setValue(node, response.version());
			node.put(DomainNode.SEQ_NO_FIELD, response.seqNo());
			node.put(DomainNode.PRIMARY_TERM_FIELD, response.primaryTerm());
			return node;
		});
	}

	private static ObjectNode read(Hit<ObjectNode> hit) {
		ObjectNode node = hit.source();
		if (hit.version() != null) {
			DomainNode.VERSION.setValue(node, hit.version());
		}
		node.put(DomainNode.SEQ_NO_FIELD, hit.seqNo());
		node.put(DomainNode.PRIMARY_TERM_FIELD, hit.primaryTerm());
		return node;
	}

	public boolean exists(String id) {
		try {
			return client.exists(e -> e.index(indexName).id(id)).value();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public int count() {
		SearchRequest request = SearchRequest.of(s ->
			s
				.index(indexName)
				.size(0)
				.trackTotalHits(t -> t.enabled(true))
		);
		return Ints.saturatedCast(search(request).hits().total().value());
	}

	public int count(Query query) {
		SearchRequest request = SearchRequest.of(s ->
			s
				.index(indexName)
				.query(query)
				.size(0)
				.trackTotalHits(t -> t.enabled(true))
		);
		return Ints.saturatedCast(search(request).hits().total().value());
	}

	public boolean close() {
		ImmutableSet<String> aliases = aliases();
		if (aliases.isEmpty()) {
			try {
				return client
					.indices()
					.close(c -> c.index(indexName))
					.acknowledged();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			return close(aliases);
		}
	}

	private ImmutableSet<String> aliases() {
		try {
			ImmutableSet.Builder<String> builder = ImmutableSet.builder();
			client
				.indices()
				.getAlias(a -> a.index(indexName))
				.result()
				.values()
				.forEach(indexAliases -> indexAliases.aliases().keySet().forEach(builder::add));
			return builder.build();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean close(Iterable<String> aliases) {
		try {
			return client
				.indices()
				.updateAliases(u -> {
					for (String alias : aliases) {
						u.actions(a -> a.remove(r -> r.index(indexName).alias(alias)));
					}
					return u;
				})
				.acknowledged();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private <T> T withSpan(String operation, Callable<T> action) {
		ISpan parent = Sentry.getSpan();
		if (parent == null) {
			try {
				return action.call();
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		ISpan span = parent.startChild("db.opensearch", indexName + " " + operation);
		try {
			T result = action.call();
			span.setStatus(SpanStatus.OK);
			return result;
		} catch (RuntimeException e) {
			span.setStatus(SpanStatus.INTERNAL_ERROR);
			throw e;
		} catch (Exception e) {
			span.setStatus(SpanStatus.INTERNAL_ERROR);
			throw new RuntimeException(e);
		} finally {
			span.finish();
		}
	}
}
