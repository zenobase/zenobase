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

import play.Logger;

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
		CreateIndexRequest request = Requests.createIndexRequest(indexName).settings(settings);
		CreateIndexResponse response = client.admin().indices().create(request).actionGet();
		Preconditions.checkState(response.acknowledged(), "Expected acknowledgement of index creation: %s", indexName);
	}

	public void delete() {
		Logger.info("Delete: %s", indexName);
		DeleteIndexRequest request = Requests.deleteIndexRequest(indexName);
		client.admin().indices().delete(request).actionGet();
	}

	public void putMapping(String typeName, ObjectNode mapping) {
		Logger.info("Mapping: %s", mapping);
		PutMappingRequest request = Requests.putMappingRequest(indexName).type(typeName).source(mapping.toString());
		client.admin().indices().putMapping(request).actionGet();
	}

	public void index(String type, String id, ObjectNode object, boolean refresh) {
		IndexRequest request = Requests.indexRequest(indexName).type(type).id(id).source(Nodes.toByteArray(object)).refresh(refresh);
		IndexResponse response = client.index(request).actionGet();
		Preconditions.checkState(id.equals(response.id()), "Expected '%s' but got '%s' after indexing", id, response.id());
	}

	public void delete(String type, String id) {
		Logger.info("Delete: %s/%s", indexName, id);
		DeleteRequest request = Requests.deleteRequest(indexName).type(type).id(id);
		client.delete(request).actionGet();
	}

	public boolean exists() {
		IndicesExistsRequest request = Requests.indicesExistsRequest(indexName);
		IndicesExistsResponse response = client.admin().indices().exists(request).actionGet();
		return response.exists();
	}

	public SearchResponse search(QueryBuilder query, SortBuilder sort, int offset, int limit, Iterable<AbstractFacetBuilder> facets) {
		SearchRequestBuilder request = client.prepareSearch(indexName)
	        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
	        .setQuery(query)
	        .setFrom(offset).setSize(limit);
		if (facets != null) {
			for (AbstractFacetBuilder facet : facets) {
				request.addFacet(facet);
			}
		}
		if (sort != null) {
			request.addSort(sort);
		}
		return request.execute().actionGet();
	}

	public void delete(QueryBuilder query) {
		DeleteByQueryRequest request = Requests.deleteByQueryRequest(indexName).query(query);
		client.deleteByQuery(request).actionGet();
	}

	public ObjectNode get(String type, String id) {
		GetResponse response = client.prepareGet(indexName, type, id).execute().actionGet();
		return response.exists() ? Nodes.read(response.source()) : null;
	}

	public long count() {
		CountResponse response = client.prepareCount(indexName).execute().actionGet();
		return response.count();
	}
}
