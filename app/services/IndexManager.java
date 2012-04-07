package services;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import schema.Schema;

import com.google.common.base.Preconditions;
import common.Callback;
import common.Nodes;

public class IndexManager {

	private final String indexName;
	private final Client client;

	public IndexManager(String indexName, Client client) {
		this.indexName = indexName;
		this.client = client;
	}

	public void create(int replicas) {
		Settings settings = ImmutableSettings.settingsBuilder()
			.put("number_of_shards", 1)
		//	.put("number_of_replicas", replicas)
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

	public void store(String type, String id, ObjectNode object, boolean refresh) {
		index(type, id, object, OpType.CREATE, refresh);
	}

	public void update(String type, String id, ObjectNode object, boolean refresh) {
		index(type, id, object, OpType.INDEX, refresh);
	}

	private void index(String type, String id, ObjectNode object, OpType operation, boolean refresh) {
		client.prepareIndex(indexName, type, id).setSource(Nodes.toByteArray(object)).setOpType(operation).setRefresh(refresh).execute().actionGet();
	}

	public void delete(String type, String id) {
		client.prepareDelete(indexName, type, id).execute().actionGet();
	}

	public boolean exists() {
		return client.admin().indices().prepareExists(indexName).execute().actionGet().exists();
	}

	public SearchResponse search(QueryBuilder query) {
		return search(new SearchSourceBuilder().query(query));
	}

	public SearchResponse search(SearchSourceBuilder search) {
		return client.search(Requests.searchRequest(indexName)
			.searchType(SearchType.DFS_QUERY_THEN_FETCH).source(search)).actionGet();
	}

	public void search(QueryBuilder query, Callback<ObjectNode> callback) {
		SearchSourceBuilder search = new SearchSourceBuilder().query(query).size(100);
		for (SearchResponse response = scroll(search); response.hits().hits().length > 0; response = scroll(response.getScrollId())) {
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
		return response.exists() ? Nodes.read(response.source()) : null;
	}

	public boolean exists(String type, String id) {
		GetResponse response = client.prepareGet(indexName, type, id).execute().actionGet();
		return response.exists();
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
