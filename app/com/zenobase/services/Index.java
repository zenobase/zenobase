package com.zenobase.services;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

import com.zenobase.common.Callback;
import com.zenobase.json.DomainNode;
import com.zenobase.json.NodeList;
import com.zenobase.json.Nodes;
import com.zenobase.json.Schema;

public class Index {

	private final TimeValue timeout = TimeValue.timeValueMillis(1000);
	private final String indexName;
	private final Client client;

	public Index(String indexName, Client client) {
		this.indexName = indexName;
		this.client = client;
	}

	public void create(int replicas) {
		Settings settings = ImmutableSettings.settingsBuilder()
			.put("number_of_shards", 1)
			.put("auto_expand_replicas", replicas == Integer.MAX_VALUE ? "0-all" : "0-" + replicas)
			.build();
		CreateIndexResponse response = client.admin().indices().prepareCreate(indexName).setSettings(settings).execute().actionGet();
		Preconditions.checkState(response.isAcknowledged(), "Expected acknowledgement of index creation: %s", indexName);
		Preconditions.checkState(new Cluster(client).isReady(), "Expected at least one shard in cluster");
	}

	public void putMapping(Schema schema) {
		client.admin().indices()
			.preparePutMapping(indexName)
			.setType(schema.getTypeName())
			.setSource(schema.toJson().toString())
			.execute().actionGet();
	}

	public void store(String type, String id, ObjectNode node, DateTime timestamp, boolean refresh) {
		index(type, id, node, OpType.CREATE, timestamp, refresh);
	}

	public void update(String type, String id, ObjectNode node, DateTime timestamp, boolean refresh) {
		index(type, id, node, OpType.INDEX, timestamp, refresh);
	}

	private void index(String type, String id, ObjectNode node, OpType operation, DateTime timestamp, boolean refresh) {
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
		long version = request.execute().actionGet().getVersion();
		DomainNode.VERSION.setValue(node, version);
	}

	public boolean delete(String type, String id, boolean refresh) {
		return !client.prepareDelete(indexName, type, id)
			.setRefresh(refresh)
			.execute().actionGet().isNotFound();
	}

	public boolean exists() {
		return client.admin().indices()
			.prepareExists(indexName)
			.execute().actionGet().isExists();
	}

	public void refresh() {
		client.admin().indices()
			.prepareRefresh(indexName)
			.execute().actionGet();
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
		return client.search(Requests.searchRequest(indexName)
			.searchType(SearchType.DFS_QUERY_THEN_FETCH).source(search)).actionGet();
	}

	public void find(QueryBuilder query, Callback<ObjectNode> callback, int scrollSize) {
		SearchSourceBuilder search = new SearchSourceBuilder().query(query).size(scrollSize);
		for (SearchResponse response = scroll(search.version(true)); response.getHits().getHits().length > 0; response = scroll(response.getScrollId())) {
			for (SearchHit hit : response.getHits()) {
				callback.call(read(hit));
			}
		}
	}

	private SearchResponse scroll(SearchSourceBuilder search) {
		SearchResponse response = client.search(Requests.searchRequest(indexName)
			.searchType(SearchType.SCAN).scroll(timeout).source(search)).actionGet();
		return scroll(response.getScrollId());
	}

	private SearchResponse scroll(String scrollId) {
		return client.searchScroll(Requests.searchScrollRequest(indexName).scrollId(scrollId).scroll(timeout)).actionGet();
	}

	public ObjectNode get(String type, String id) {
		GetResponse response = client.prepareGet(indexName, type, id).execute().actionGet();
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
			.execute().actionGet().isExists();
	}

	public int count() {
		return Ints.saturatedCast(client
			.prepareCount(indexName)
			.execute().actionGet().getCount());
	}

	public void open() {
		client.admin().indices()
			.prepareOpen(indexName)
			.execute().actionGet();
	}

	public void close() {
		client.admin().indices()
			.prepareClose(indexName)
			.execute().actionGet();
	}
}
