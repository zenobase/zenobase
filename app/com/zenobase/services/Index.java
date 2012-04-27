package com.zenobase.services;

import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
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
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.common.Callback;
import com.zenobase.common.Nodes;
import com.zenobase.common.PartialList;
import com.zenobase.json.Schema;
import com.zenobase.models.DomainNode;

public class Index {

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
		Preconditions.checkState(response.acknowledged(), "Expected acknowledgement of index creation: %s", indexName);
	}

	public void delete() {
		DeleteIndexRequest request = Requests.deleteIndexRequest(indexName);
		client.admin().indices().delete(request).actionGet();
	}

	public void putMapping(Schema schema) {
		// Logger.info("Mapping: " + schema.toJson());
		client.admin().indices().preparePutMapping(indexName).setType(schema.getTypeName()).setSource(schema.toJson().toString()).execute().actionGet();
	}

	public void store(String type, String id, ObjectNode node, boolean refresh) {
		index(type, id, node, OpType.CREATE, refresh);
	}

	public void update(String type, String id, ObjectNode node, boolean refresh) {
		index(type, id, node, OpType.INDEX, refresh);
	}

	private void index(String type, String id, ObjectNode node, OpType operation, boolean refresh) {
		IndexRequestBuilder request = client.prepareIndex(indexName, type, id);
		if (operation == OpType.INDEX) {
			Long version = DomainNode.VERSION.getValue(node);
			if (version != null) {
				request.setVersion(version);
			}
		}
		request.setSource(Nodes.toByteArray(node));
		request.setOpType(operation);
		request.setRefresh(refresh);
		request.execute().actionGet();
	}

	public void delete(String type, String id, boolean refresh) {
		client.prepareDelete(indexName, type, id).setRefresh(refresh).execute().actionGet();
	}

	public boolean exists() {
		return client.admin().indices().prepareExists(indexName).execute().actionGet().exists();
	}

	public PartialList<ObjectNode> find(QueryBuilder query) {
		return find(new SearchSourceBuilder().query(query));
	}

	public PartialList<ObjectNode> find(SearchSourceBuilder search) {
		SearchResponse response = search(search);
		SearchHits hits = response.hits();
		List<ObjectNode> nodes = Lists.newArrayListWithCapacity(hits.hits().length);
		for (SearchHit hit : hits) {
			nodes.add(Nodes.read(hit.source()));
		}
		return new PartialList<ObjectNode>(nodes, hits.totalHits());
	}

	public SearchResponse search(SearchSourceBuilder search) {
		return client.search(Requests.searchRequest(indexName)
			.searchType(SearchType.DFS_QUERY_THEN_FETCH).source(search)).actionGet();
	}

	public void find(QueryBuilder query, Callback<ObjectNode> callback) {
		SearchSourceBuilder search = new SearchSourceBuilder().query(query).size(100);
		for (SearchResponse response = scroll(search.version(true)); response.hits().hits().length > 0; response = scroll(response.getScrollId())) {
			for (SearchHit hit : response.hits()) {
				callback.call(Nodes.read(hit.source()));
			}
		}
	}

	private SearchResponse scroll(SearchSourceBuilder search) {
		final TimeValue timeout = TimeValue.timeValueMillis(1000);
		SearchResponse response = client.search(Requests.searchRequest(indexName)
			.searchType(SearchType.SCAN).scroll(timeout).source(search)).actionGet();
		return scroll(response.getScrollId());
	}

	private SearchResponse scroll(String scrollId) {
		final TimeValue timeout = TimeValue.timeValueMillis(1000);
		return client.searchScroll(Requests.searchScrollRequest(indexName).scrollId(scrollId).scroll(timeout)).actionGet();
	}

	public void delete(QueryBuilder query) {
		client.prepareDeleteByQuery(indexName).setQuery(query).execute().actionGet();
	}

	public ObjectNode get(String type, String id) {
		GetResponse response = client.prepareGet(indexName, type, id).execute().actionGet();
		return response.exists() ? read(response.source(), response.version()) : null;
	}

	private static ObjectNode read(byte[] source, long version) {
		ObjectNode node = Nodes.read(source);
		if (version > 0) {
			DomainNode.VERSION.setValue(node, version);
		}
		return node;
	}

	public boolean exists(String type, String id) {
		return client.prepareGet(indexName, type, id).execute().actionGet().exists();
	}

	public long count() {
		return client.prepareCount(indexName).execute().actionGet().count();
	}

	public void open() {
		client.admin().indices().prepareOpen(indexName).execute().actionGet();
	}

	public void close() {
		client.admin().indices().prepareClose(indexName).execute().actionGet();
	}
}
