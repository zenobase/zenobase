package com.zenobase.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.OpType;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.Time;
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
import org.joda.time.DateTime;

import com.zenobase.common.Callback;
import com.zenobase.json.DomainNode;
import com.zenobase.json.NodeList;
import com.zenobase.json.Schema;

public class Index {

	private static final Logger logger = LoggerFactory.getLogger(Index.class);

	private static final Time SCROLL_TIMEOUT = Time.of(t -> t.time("5m"));
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
			boolean acknowledged = client.indices().create(c -> c
				.index(indexName)
				.settings(s -> s
					.numberOfShards(shards)
					.autoExpandReplicas(autoExpandReplicas)
				)
			).acknowledged();
			Preconditions.checkState(acknowledged, "Expected acknowledgement of index creation: %s", indexName);
			Preconditions.checkState(new Cluster(client).isReady(), "Expected at least one shard in cluster");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void putMapping(Schema schema) {
		try {
			String json = schema.toJson().toString();
			client.generic().execute(
				org.opensearch.client.opensearch.generic.Requests.builder()
					.endpoint(indexName + "/_mapping")
					.method("PUT")
					.json(json)
					.build()
			).close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void store(String id, ObjectNode node, DateTime timestamp, boolean refresh) {
		index(id, node, OpType.Create, timestamp, refresh);
	}

	public void store(List<? extends DomainNode> nodes, DateTime timestamp, boolean refresh) {
		index(nodes, OpType.Create, timestamp, refresh);
	}

	public void update(String id, ObjectNode node, DateTime timestamp, boolean refresh) {
		index(id, node, OpType.Index, timestamp, refresh);
	}

	private void index(String id, ObjectNode node, OpType operation, DateTime timestamp, boolean refresh) {
		try {
			IndexRequest.Builder<ObjectNode> builder = new IndexRequest.Builder<ObjectNode>()
				.index(indexName)
				.id(id)
				.document(stripMetadata(node))
				.opType(operation)
				.refresh(refreshPolicy(refresh));
			if (operation == OpType.Index) {
				Long seqNo = DomainNode.SEQ_NO.getValue(node);
				Long primaryTerm = DomainNode.PRIMARY_TERM.getValue(node);
				Preconditions.checkNotNull(seqNo, "Missing seq_no field: %s", node);
				Preconditions.checkNotNull(primaryTerm, "Missing primary_term field: %s", node);
				builder.ifSeqNo(seqNo);
				builder.ifPrimaryTerm(primaryTerm);
			}
			IndexResponse response = client.index(builder.build());
			DomainNode.VERSION.setValue(node, response.version());
			DomainNode.SEQ_NO.setValue(node, response.seqNo());
			DomainNode.PRIMARY_TERM.setValue(node, response.primaryTerm());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void index(List<? extends DomainNode> nodes, OpType operation, DateTime timestamp, boolean refresh) {
		int BATCH_SIZE = 10000;
		for (int begin = 0; begin < nodes.size(); begin += BATCH_SIZE) {
			List<BulkOperation> operations = new ArrayList<>();
			for (int i = 0; i < BATCH_SIZE && begin + i < nodes.size(); ++i) {
				DomainNode node = nodes.get(begin + i);
				ObjectNode doc = stripMetadata(node.toJson());
				BulkOperation.Builder opBuilder = new BulkOperation.Builder();
				if (operation == OpType.Create) {
					opBuilder.create(c -> c.index(indexName).id(node.getId()).document(doc));
				} else {
					Long seqNo = DomainNode.SEQ_NO.getValue(node.toJson());
					Long primaryTerm = DomainNode.PRIMARY_TERM.getValue(node.toJson());
					Preconditions.checkNotNull(seqNo, "Missing seq_no field: %s", node.toJson());
					Preconditions.checkNotNull(primaryTerm, "Missing primary_term field: %s", node.toJson());
					opBuilder.index(idx -> idx.index(indexName).id(node.getId()).document(doc)
						.ifSeqNo(seqNo).ifPrimaryTerm(primaryTerm));
				}
				operations.add(opBuilder.build());
			}
			try {
				BulkResponse bulkResponse = client.bulk(b -> b
					.operations(operations)
					.refresh(refreshPolicy(refresh))
				);
				List<BulkResponseItem> items = bulkResponse.items();
				String failureMessage = getFailureMessage(items);
				if (failureMessage != null) {
					List<String> failed = new ArrayList<>();
					for (int i = 0; i < items.size(); ++i) {
						if (items.get(i).error() == null) {
							failed.add(nodes.get(begin + i).getId());
						}
					}
					if (!failed.isEmpty()) {
						delete(failed, refresh);
					}
					throw new RuntimeException("Couldn't store an item: " + failureMessage);
				}
				for (int i = 0; i < items.size(); ++i) {
					DomainNode node = nodes.get(begin + i);
					node.setVersion(items.get(i).version());
					DomainNode.SEQ_NO.setValue(node.toJson(), items.get(i).seqNo());
					DomainNode.PRIMARY_TERM.setValue(node.toJson(), items.get(i).primaryTerm());
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static String getFailureMessage(List<BulkResponseItem> items) {
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
		copy.remove(DomainNode.SEQ_NO.getName());
		copy.remove(DomainNode.PRIMARY_TERM.getName());
		return copy;
	}

	public boolean delete(String id, boolean refresh) {
		try {
			return client.delete(d -> d
				.index(indexName)
				.id(id)
				.refresh(refreshPolicy(refresh))
			).result().jsonValue().equals("deleted");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean delete(List<String> ids, boolean refresh) {
		List<BulkOperation> operations = new ArrayList<>();
		for (String id : ids) {
			operations.add(BulkOperation.of(op -> op.delete(d -> d.index(indexName).id(id))));
		}
		try {
			BulkResponse bulkResponse = client.bulk(b -> b.operations(operations).refresh(refreshPolicy(refresh)));
			String failureMessage = getFailureMessage(bulkResponse.items());
			if (failureMessage != null) {
				throw new RuntimeException("Couldn't delete an item: " + failureMessage);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	public boolean exists() {
		try {
			return client.indices().exists(e -> e.index(indexName)).value();
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
		SearchRequest request = SearchRequest.of(s -> s
			.index(indexName)
			.query(query)
			.version(true)
			.seqNoPrimaryTerm(true)
		);
		SearchResponse<ObjectNode> response = search(request);
		List<ObjectNode> nodes = new ArrayList<>(response.hits().hits().size());
		for (Hit<ObjectNode> hit : response.hits().hits()) {
			nodes.add(read(hit));
		}
		return new NodeList(nodes, response.hits().total().value());
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
		try {
			return client.search(request, ObjectNode.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void find(Query query, Callback<ObjectNode> callback, int scrollSize) {
		SearchRequest.Builder builder = new SearchRequest.Builder()
			.index(indexName)
			.query(query)
			.size(scrollSize)
			.version(true)
			.seqNoPrimaryTerm(true);
		find(builder, callback);
	}

	public void find(SearchRequest.Builder requestBuilder, Callback<ObjectNode> callback) {
		try {
			SearchRequest searchRequest = requestBuilder.scroll(SCROLL_TIMEOUT).build();
			SearchResponse<ObjectNode> response = client.search(searchRequest, ObjectNode.class);
			while (!response.hits().hits().isEmpty()) {
				for (Hit<ObjectNode> hit : response.hits().hits()) {
					callback.call(read(hit));
				}
				String scrollId = response.scrollId();
				response = scrollWithRetry(scrollId);
			}
			String finalScrollId = response.scrollId();
			client.clearScroll(c -> c.scrollId(finalScrollId));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private SearchResponse<ObjectNode> scrollWithRetry(String scrollId) throws IOException {
		final int maxScrollAttempts = 5;
		OpenSearchException lastException = null;
		for (int attempt = 1; attempt <= maxScrollAttempts; attempt++) {
			try {
				return client.scroll(sr -> sr.scrollId(scrollId).scroll(SCROLL_TIMEOUT), ObjectNode.class);
			} catch (OpenSearchException e) {
				lastException = e;
				logger.warn("Scroll attempt {}/{} failed: {}", attempt, maxScrollAttempts, e.getMessage());
				try {
					Thread.sleep(attempt * 5000L);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
		throw new RuntimeException(lastException);
	}

	public ObjectNode get(String id) {
		try {
			GetResponse<ObjectNode> response = client.get(g -> g.index(indexName).id(id), ObjectNode.class);
			if (!response.found()) return null;
			ObjectNode node = response.source();
			DomainNode.VERSION.setValue(node, response.version());
			DomainNode.SEQ_NO.setValue(node, response.seqNo());
			DomainNode.PRIMARY_TERM.setValue(node, response.primaryTerm());
			return node;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static ObjectNode read(Hit<ObjectNode> hit) {
		ObjectNode node = hit.source();
		if (hit.version() != null) {
			DomainNode.VERSION.setValue(node, hit.version());
		}
		DomainNode.SEQ_NO.setValue(node, hit.seqNo());
		DomainNode.PRIMARY_TERM.setValue(node, hit.primaryTerm());
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
		SearchRequest request = SearchRequest.of(s -> s.index(indexName).size(0).trackTotalHits(t -> t.enabled(true)));
		return Ints.saturatedCast(search(request).hits().total().value());
	}

	public int count(Query query) {
		SearchRequest request = SearchRequest.of(s -> s.index(indexName).query(query).size(0).trackTotalHits(t -> t.enabled(true)));
		return Ints.saturatedCast(search(request).hits().total().value());
	}

	public boolean close() {
		Set<String> aliases = aliases();
		if (aliases.isEmpty()) {
			try {
				return client.indices().close(c -> c.index(indexName)).acknowledged();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			return close(aliases);
		}
	}

	private Set<String> aliases() {
		try {
			ImmutableSet.Builder<String> builder = ImmutableSet.builder();
			client.indices().getAlias(a -> a.index(indexName))
				.result().values().forEach(indexAliases ->
					indexAliases.aliases().keySet().forEach(builder::add));
			return builder.build();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean close(Iterable<String> aliases) {
		try {
			return client.indices().updateAliases(u -> {
				for (String alias : aliases) {
					u.actions(a -> a.remove(r -> r.index(indexName).alias(alias)));
				}
				return u;
			}).acknowledged();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
