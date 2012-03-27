package services;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.google.common.base.Preconditions;
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

	public void putMapping(String typeName, ObjectNode mapping) {
		client.admin().indices().preparePutMapping(indexName).setType(typeName).setSource(mapping.toString()).execute().actionGet();
	}

	public void index(String type, String id, ObjectNode object, boolean refresh) {
		client.prepareIndex(indexName, type, id).setSource(Nodes.toByteArray(object)).setRefresh(refresh).execute().actionGet();
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

	public void delete(QueryBuilder query) {
		client.prepareDeleteByQuery(indexName).setQuery(query).execute().actionGet();
	}

	public ObjectNode get(String type, String id) {
		GetResponse response = client.prepareGet(indexName, type, id).execute().actionGet();
		return response.exists() ? Nodes.read(response.source()) : null;
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
