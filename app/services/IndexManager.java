package services;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.facet.AbstractFacetBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;

import play.Logger;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import common.Nodes;

public class IndexManager {

	private final String indexName;
	private final Client client;

	public IndexManager(String indexName, Client client) {
		this.indexName = indexName;
		this.client = client;
	}

	public void create(int shards, int replicas) {
		Logger.info("Creating index '%s'...", indexName);
		Settings settings = ImmutableSettings.settingsBuilder()
			.put("number_of_shards", shards)
			.put("number_of_replicas", replicas).build();
		CreateIndexResponse response = client.admin().indices().prepareCreate(indexName).setSettings(settings).execute().actionGet();
		Preconditions.checkState(response.acknowledged(), "Expected acknowledgement of index creation: %s", indexName);
	}

	public void delete() {
		Logger.info("Delete: %s", indexName);
		DeleteIndexRequest request = Requests.deleteIndexRequest(indexName);
		client.admin().indices().delete(request).actionGet();
	}

	public void putMapping(String typeName, ObjectNode mapping) {
		Logger.info("Mapping: %s", mapping);
		client.admin().indices().preparePutMapping(indexName).setType(typeName).setSource(mapping.toString()).execute().actionGet();
	}

	public void index(String type, String id, ObjectNode object, boolean refresh) {
		client.prepareIndex(indexName, type, id).setSource(Nodes.toByteArray(object)).setRefresh(refresh).execute().actionGet();
	}

	public void delete(String type, String id) {
		Logger.info("Delete: %s/%s", indexName, id);
		client.prepareDelete(indexName, type, id).execute().actionGet();
	}

	public boolean exists() {
		return client.admin().indices().prepareExists(indexName).execute().actionGet().exists();
	}

	public SearchRequestBuilder prepareSearch(QueryBuilder query, SortBuilder sort, int offset, int limit) {
		return client.prepareSearch(indexName)
	        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
	        .setQuery(query)
	        .setFrom(offset).setSize(limit).addSort(Objects.firstNonNull(sort, SortBuilders.scoreSort()));
	}

	public SearchResponse search(SearchRequestBuilder request) {
		return request.execute().actionGet();
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
}
