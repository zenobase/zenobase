package com.zenobase.services;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;

import com.zenobase.common.Callback;
import com.zenobase.json.DomainNode;
import com.zenobase.json.NodeList;
import com.zenobase.json.Nodes;
import com.zenobase.json.Schema;

public class Index {

	private final TimeValue timeout = TimeValue.timeValueMinutes(5);
	private final String indexName;
	private final Client client;

	public Index(String indexName, Client client) {
		this.indexName = indexName;
		this.client = client;
	}

	public void create(int replicas) {
		create(1, replicas);
	}

	public void create(int shards, int replicas) {
		Settings settings = ImmutableSettings.settingsBuilder()
			.put("number_of_shards", shards)
			.put("auto_expand_replicas", replicas == Integer.MAX_VALUE ? "0-all" : "0-" + replicas)
			.build();
		CreateIndexResponse response = client.admin().indices().prepareCreate(indexName).setSettings(settings).get();
		Preconditions.checkState(response.isAcknowledged(), "Expected acknowledgement of index creation: %s", indexName);
		Preconditions.checkState(new Cluster(client).isReady(), "Expected at least one shard in cluster");
	}

	public void putMapping(Schema schema) {
		client.admin().indices()
			.preparePutMapping(indexName)
			.setType(schema.getTypeName())
			.setSource(schema.toJson().toString())
			.get();
	}

	public void store(String type, String id, ObjectNode node, DateTime timestamp, boolean refresh) {
		index(type, id, node, OpType.CREATE, timestamp, refresh);
	}

	public void store(String type, List<? extends DomainNode> nodes, DateTime timestamp, boolean refresh) {
		index(type, nodes, OpType.CREATE, timestamp, refresh);
	}

	public void update(String type, String id, ObjectNode node, DateTime timestamp, boolean refresh) {
		index(type, id, node, OpType.INDEX, timestamp, refresh);
	}

	private void index(String type, String id, ObjectNode node, OpType operation, DateTime timestamp, boolean refresh) {
		IndexRequestBuilder request = buildIndexRequest(type, id, node, operation, timestamp, refresh);
		long version = request.get().getVersion();
		DomainNode.VERSION.setValue(node, version);
	}

	private void index(String type, List<? extends DomainNode> nodes, OpType operation, DateTime timestamp, boolean refresh) {
		final int BATCH_SIZE = 10000;
		for (int begin = 0; begin < nodes.size(); begin += BATCH_SIZE) {
			BulkRequestBuilder request = client.prepareBulk();
			for (int i = 0; i < BATCH_SIZE && begin + i < nodes.size(); ++i) {
				DomainNode node = nodes.get(begin + i);
				request.add(buildIndexRequest(type, node.getId(), node.toJson(), operation, timestamp, refresh));
			}
			request.setRefresh(refresh);
			BulkItemResponse[] responses = request.get().getItems();
			String failureMessage = getFailureMessage(responses);
			if (failureMessage != null) {
				List<String> failed = Lists.newArrayList();
				for (int i = 0; i < responses.length; ++i) {
					if (!responses[i].isFailed()) {
						failed.add(nodes.get(i).getId());
					}
				}
				delete(type, failed, refresh);
				throw new RuntimeException("Couldn't store an item: " + failureMessage);
			}
			for (int i = 0; i < responses.length; ++i) {
				long version = responses[i].getVersion();
				nodes.get(begin + i).setVersion(version);
			}
		}
	}

	private static String getFailureMessage(BulkItemResponse[] responses) {
		for (int i = 0; i < responses.length; ++i) {
			if (responses[i].isFailed()) {
				return responses[i].getFailureMessage();
			}
		}
		return null;
	}

	private IndexRequestBuilder buildIndexRequest(String type, String id, ObjectNode node, OpType operation, DateTime timestamp, boolean refresh) {
		IndexRequestBuilder request = client.prepareIndex(indexName, type, id);
		if (operation == OpType.INDEX) {
			Long version = DomainNode.VERSION.getValue(node);
			Preconditions.checkNotNull(version, "Missing a version field: %s", node);
			request.setVersion(version);
		}
		request.setSource(Nodes.toByteArray(node));
		request.setOpType(operation);
		request.setTimestamp(timestamp.toString());
		request.setRefresh(refresh);
		return request;
	}

	public boolean delete(String type, String id, boolean refresh) {
		return client.prepareDelete(indexName, type, id)
			.setRefresh(refresh)
			.get().isFound();
	}

	public boolean delete(String type, List<String> ids, boolean refresh) {
		BulkRequestBuilder request = client.prepareBulk().setRefresh(refresh);
		for (String id : ids) {
			request.add(client.prepareDelete(indexName, type, id));
		}
		request.get();
		return true;
	}

	public boolean exists() {
		return client.admin().indices()
			.prepareExists(indexName)
			.get().isExists();
	}

	public void refresh() {
		client.admin().indices()
			.prepareRefresh(indexName)
			.get();
	}

	public NodeList find(QueryBuilder query) {
		return find(new SearchSourceBuilder().query(query).version(Boolean.TRUE));
	}

	public NodeList find(SearchSourceBuilder search) {
		SearchResponse response = search(search);
		SearchHits hits = response.getHits();
		List<ObjectNode> nodes = Lists.newArrayListWithCapacity(hits.getHits().length);
		for (SearchHit hit : hits) {
			nodes.add(read(hit));
		}
		return new NodeList(nodes, hits.totalHits());
	}

	public SearchResponse search(SearchSourceBuilder search) {
		return client.prepareSearch(indexName)
			.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
			.setSource(search.buildAsBytes())
			.get();
	}

	public void find(QueryBuilder query, Callback<ObjectNode> callback, int scrollSize) {
		find(new SearchSourceBuilder().query(query).size(scrollSize).version(true), callback);
	}

	private void find(SearchSourceBuilder search, Callback<ObjectNode> callback) {
		SearchResponse response;
		for (response = scroll(search); response.getHits().getHits().length > 0; response = scroll(response.getScrollId())) {
			for (SearchHit hit : response.getHits()) {
				callback.call(read(hit));
			}
		}
		clearScroll(response.getScrollId());
	}

	private SearchResponse scroll(SearchSourceBuilder search) {
		SearchResponse response = client.prepareSearch(indexName)
			.setSearchType(SearchType.SCAN)
			.setScroll(timeout)
			.setSource(search.buildAsBytes())
			.get();
		return scroll(response.getScrollId());
	}

	private SearchResponse scroll(String scrollId) {
		return client.prepareSearchScroll(scrollId).setScroll(timeout).get();
	}

	private void clearScroll(String scrollId) {
		client.prepareClearScroll().addScrollId(scrollId).get();
	}

	public ObjectNode get(String type, String id) {
		GetResponse response = client.prepareGet(indexName, type, id).get();
		return response.isExists() ? read(response.getSourceAsBytes(), response.getVersion()) : null;
	}

	private static ObjectNode read(SearchHit hit) {
		return read(hit.source(), hit.getVersion());
	}

	private static ObjectNode read(byte[] source, long version) {
		ObjectNode node = Nodes.readObject(source);
		if (version > 0) {
			DomainNode.VERSION.setValue(node, version);
		}
		return node;
	}

	public boolean exists(String type, String id) {
		return client
			.prepareGet(indexName, type, id)
			.get().isExists();
	}

	public int count() {
		return Ints.saturatedCast(client
			.prepareCount(indexName)
			.get().getCount());
	}

	public int count(QueryBuilder query) {
		return Ints.saturatedCast(client
			.prepareCount(indexName)
			.setQuery(query)
			.get().getCount());
	}

	public void open() {
		client.admin().indices()
			.prepareOpen(indexName)
			.get();
	}

	public boolean close() {
		Set<String> aliases = aliases();
		if (aliases.isEmpty()) {
			return client.admin().indices()
				.prepareClose(indexName)
				.get().isAcknowledged();
		} else {
			return close(aliases);
		}
	}

	private Set<String> aliases() {
		return ImmutableSet.copyOf(client.admin().indices()
			.prepareGetAliases(indexName)
			.get().getAliases().keysIt());
	}

	private boolean close(Iterable<String> aliases) {
		IndicesAliasesRequestBuilder request = client.admin().indices().prepareAliases();
		for (String alias : aliases()) {
			request.removeAlias(alias, indexName);
		}
		return request.get().isAcknowledged();
	}
}
