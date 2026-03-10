package com.zenobase.services;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import org.opensearch.action.DocWriteRequest;
import org.opensearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.opensearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.opensearch.client.indices.CloseIndexRequest;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.ClearScrollRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchScrollRequest;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.client.GetAliasesResponse;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.CreateIndexResponse;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.client.indices.PutMappingRequest;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.reindex.DeleteByQueryRequest;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.action.admin.indices.refresh.RefreshRequest;
import org.opensearch.action.admin.indices.flush.FlushRequest;
import org.joda.time.DateTime;

import com.zenobase.common.Callback;
import com.zenobase.json.DomainNode;
import com.zenobase.json.NodeList;
import com.zenobase.json.Nodes;
import com.zenobase.json.Schema;

public class Index {

	private final TimeValue timeout = TimeValue.timeValueMinutes(5);
	private final String indexName;
	private final RestHighLevelClient client;

	public Index(String indexName, RestHighLevelClient client) {
		this.indexName = indexName;
		this.client = client;
	}

	public void create(int replicas) {
		create(1, replicas);
	}

	public void create(int shards, int replicas) {
		Settings settings = Settings.builder()
			.put("number_of_shards", shards)
			.put("auto_expand_replicas", replicas == Integer.MAX_VALUE ? "0-all" : "0-" + replicas)
			.build();
		try {
			CreateIndexRequest request = new CreateIndexRequest(indexName).settings(settings);
			CreateIndexResponse response = client.indices().create(request, TypeInjectingInterceptor.OPTIONS);
			Preconditions.checkState(response.isAcknowledged(), "Expected acknowledgement of index creation: %s", indexName);
			Preconditions.checkState(new Cluster(client).isReady(), "Expected at least one shard in cluster");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void putMapping(Schema schema) {
		try {
			PutMappingRequest request = new PutMappingRequest(indexName)
				.source(schema.toJson().toString(), XContentType.JSON);
			client.indices().putMapping(request, TypeInjectingInterceptor.OPTIONS);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void store(String type, String id, ObjectNode node, DateTime timestamp, boolean refresh) {
		index(type, id, node, DocWriteRequest.OpType.CREATE, timestamp, refresh);
	}

	public void store(String type, List<? extends DomainNode> nodes, DateTime timestamp, boolean refresh) {
		index(type, nodes, DocWriteRequest.OpType.CREATE, timestamp, refresh);
	}

	public void update(String type, String id, ObjectNode node, DateTime timestamp, boolean refresh) {
		index(type, id, node, DocWriteRequest.OpType.INDEX, timestamp, refresh);
	}

	private void index(String type, String id, ObjectNode node, DocWriteRequest.OpType operation, DateTime timestamp, boolean refresh) {
		IndexRequest request = buildIndexRequest(type, id, node, operation, timestamp, refresh);
		try {
			org.opensearch.action.index.IndexResponse response = client.index(request, TypeInjectingInterceptor.OPTIONS);
			DomainNode.VERSION.setValue(node, response.getVersion());
			DomainNode.SEQ_NO.setValue(node, response.getSeqNo());
			DomainNode.PRIMARY_TERM.setValue(node, response.getPrimaryTerm());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void index(String type, List<? extends DomainNode> nodes, DocWriteRequest.OpType operation, DateTime timestamp, boolean refresh) {
		int BATCH_SIZE = 10000;
		for (int begin = 0; begin < nodes.size(); begin += BATCH_SIZE) {
			BulkRequest request = new BulkRequest();
			for (int i = 0; i < BATCH_SIZE && begin + i < nodes.size(); ++i) {
				DomainNode node = nodes.get(begin + i);
				request.add(buildIndexRequest(type, node.getId(), node.toJson(), operation, timestamp, refresh));
			}
			request.setRefreshPolicy(refresh ? WriteRequest.RefreshPolicy.IMMEDIATE : WriteRequest.RefreshPolicy.NONE);
			try {
				BulkResponse bulkResponse = client.bulk(request, TypeInjectingInterceptor.OPTIONS);
				BulkItemResponse[] responses = bulkResponse.getItems();
				String failureMessage = getFailureMessage(responses);
				if (failureMessage != null) {
					List<String> failed = Lists.newArrayList();
					for (int i = 0; i < responses.length; ++i) {
						if (!responses[i].isFailed()) {
							failed.add(nodes.get(begin + i).getId());
						}
					}
					if (!failed.isEmpty()) {
						delete(type, failed, refresh);
					}
					throw new RuntimeException("Couldn't store an item: " + failureMessage);
				}
				for (int i = 0; i < responses.length; ++i) {
					DomainNode node = nodes.get(begin + i);
					node.setVersion(responses[i].getVersion());
					DomainNode.SEQ_NO.setValue(node.toJson(), responses[i].getResponse().getSeqNo());
					DomainNode.PRIMARY_TERM.setValue(node.toJson(), responses[i].getResponse().getPrimaryTerm());
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static String getFailureMessage(BulkItemResponse[] responses) {
		for (BulkItemResponse response : responses) {
			if (response.isFailed()) {
				return response.getFailureMessage();
			}
		}
		return null;
	}

	private IndexRequest buildIndexRequest(String type, String id, ObjectNode node, DocWriteRequest.OpType operation, DateTime timestamp, boolean refresh) {
		IndexRequest request = new IndexRequest(indexName).id(id);
		if (operation == DocWriteRequest.OpType.INDEX) {
			Long seqNo = DomainNode.SEQ_NO.getValue(node);
			Long primaryTerm = DomainNode.PRIMARY_TERM.getValue(node);
			Preconditions.checkNotNull(seqNo, "Missing seq_no field: %s", node);
			Preconditions.checkNotNull(primaryTerm, "Missing primary_term field: %s", node);
			request.setIfSeqNo(seqNo);
			request.setIfPrimaryTerm(primaryTerm);
		}
		request.source(Nodes.toByteArray(stripMetadata(node)), XContentType.JSON);
		request.opType(operation);
		request.setRefreshPolicy(refresh ? WriteRequest.RefreshPolicy.IMMEDIATE : WriteRequest.RefreshPolicy.NONE);
		return request;
	}

	private static ObjectNode stripMetadata(ObjectNode node) {
		ObjectNode copy = node.deepCopy();
		copy.remove(DomainNode.VERSION.getName());
		copy.remove(DomainNode.SEQ_NO.getName());
		copy.remove(DomainNode.PRIMARY_TERM.getName());
		return copy;
	}

	public boolean delete(String type, String id, boolean refresh) {
		try {
			DeleteRequest request = new DeleteRequest(indexName, id)
				.setRefreshPolicy(refresh ? WriteRequest.RefreshPolicy.IMMEDIATE : WriteRequest.RefreshPolicy.NONE);
			return client.delete(request, TypeInjectingInterceptor.OPTIONS).getResult() == org.opensearch.action.DocWriteResponse.Result.DELETED;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean delete(String type, List<String> ids, boolean refresh) {
		BulkRequest request = new BulkRequest()
			.setRefreshPolicy(refresh ? WriteRequest.RefreshPolicy.IMMEDIATE : WriteRequest.RefreshPolicy.NONE);
		for (String id : ids) {
			request.add(new DeleteRequest(indexName, id));
		}
		try {
			client.bulk(request, TypeInjectingInterceptor.OPTIONS);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	public boolean exists() {
		try {
			return client.indices().exists(new GetIndexRequest(indexName), TypeInjectingInterceptor.OPTIONS);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void refresh() {
		try {
			client.indices().refresh(new RefreshRequest(indexName), TypeInjectingInterceptor.OPTIONS);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public NodeList find(QueryBuilder query) {
		return find(new SearchSourceBuilder().query(query).version(Boolean.TRUE).seqNoAndPrimaryTerm(Boolean.TRUE));
	}

	public NodeList find(SearchSourceBuilder search) {
		search.seqNoAndPrimaryTerm(Boolean.TRUE);
		SearchResponse response = search(search);
		SearchHits hits = response.getHits();
		List<ObjectNode> nodes = Lists.newArrayListWithCapacity(hits.getHits().length);
		for (SearchHit hit : hits) {
			nodes.add(read(hit));
		}
		return new NodeList(nodes, hits.getTotalHits().value);
	}

	public SearchResponse search(SearchSourceBuilder search) {
		try {
			SearchRequest request = new SearchRequest(indexName).source(search);
			return client.search(request, TypeInjectingInterceptor.OPTIONS);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void find(QueryBuilder query, Callback<ObjectNode> callback, int scrollSize) {
		find(new SearchSourceBuilder().query(query).size(scrollSize).version(true).seqNoAndPrimaryTerm(true), callback);
	}

	private void find(SearchSourceBuilder search, Callback<ObjectNode> callback) {
		try {
			SearchRequest searchRequest = new SearchRequest(indexName)
				.source(search)
				.scroll(timeout);
			SearchResponse response = client.search(searchRequest, TypeInjectingInterceptor.OPTIONS);
			while (response.getHits().getHits().length > 0) {
				for (SearchHit hit : response.getHits()) {
					callback.call(read(hit));
				}
				SearchScrollRequest scrollRequest = new SearchScrollRequest(response.getScrollId()).scroll(timeout);
				response = client.scroll(scrollRequest, TypeInjectingInterceptor.OPTIONS);
			}
			ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
			clearScrollRequest.addScrollId(response.getScrollId());
			client.clearScroll(clearScrollRequest, TypeInjectingInterceptor.OPTIONS);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public ObjectNode get(String type, String id) {
		try {
			GetResponse response = client.get(new GetRequest(indexName, id), TypeInjectingInterceptor.OPTIONS);
			if (!response.isExists()) return null;
			ObjectNode node = read(response.getSourceAsBytes(), response.getVersion());
			DomainNode.SEQ_NO.setValue(node, response.getSeqNo());
			DomainNode.PRIMARY_TERM.setValue(node, response.getPrimaryTerm());
			return node;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static ObjectNode read(SearchHit hit) {
		ObjectNode node = read(hit.getSourceRef().toBytesRef().bytes, hit.getVersion());
		DomainNode.SEQ_NO.setValue(node, hit.getSeqNo());
		DomainNode.PRIMARY_TERM.setValue(node, hit.getPrimaryTerm());
		return node;
	}

	private static ObjectNode read(byte[] source, long version) {
		ObjectNode node = Nodes.readObject(source);
		if (version > 0) {
			DomainNode.VERSION.setValue(node, version);
		}
		return node;
	}

	public boolean exists(String type, String id) {
		try {
			return client.get(new GetRequest(indexName, id), TypeInjectingInterceptor.OPTIONS).isExists();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public int count() {
		SearchSourceBuilder search = new SearchSourceBuilder().size(0).trackTotalHits(true);
		return Ints.saturatedCast(search(search).getHits().getTotalHits().value);
	}

	public int count(QueryBuilder query) {
		SearchSourceBuilder search = new SearchSourceBuilder().query(query).size(0).trackTotalHits(true);
		return Ints.saturatedCast(search(search).getHits().getTotalHits().value);
	}

	public boolean close() {
		Set<String> aliases = aliases();
		if (aliases.isEmpty()) {
			try {
				return client.indices().close(new CloseIndexRequest(indexName), TypeInjectingInterceptor.OPTIONS).isAcknowledged();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			return close(aliases);
		}
	}

	private Set<String> aliases() {
		try {
			GetAliasesRequest request = new GetAliasesRequest().indices(indexName);
			GetAliasesResponse response = client.indices().getAlias(request, TypeInjectingInterceptor.OPTIONS);
			ImmutableSet.Builder<String> builder = ImmutableSet.builder();
			response.getAliases().values().forEach(aliasSet -> aliasSet.forEach(meta -> builder.add(meta.alias())));
			return builder.build();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean close(Iterable<String> aliases) {
		IndicesAliasesRequest request = new IndicesAliasesRequest();
		for (String alias : aliases) {
			request.addAliasAction(new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
				.index(indexName).alias(alias));
		}
		try {
			return client.indices().updateAliases(request, TypeInjectingInterceptor.OPTIONS).isAcknowledged();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
