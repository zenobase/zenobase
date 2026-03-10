package com.zenobase.search;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchAllQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import com.zenobase.common.Callback;
import com.zenobase.json.IntegerField;
import com.zenobase.json.Nodes;
import com.zenobase.services.Index;

public class Search {

	public static final IntegerField TOTAL = new IntegerField("total");

	private final ImmutableSet<Facet> facets;
	private final ImmutableList<Query> must, mustNot;

	public Search(Iterable<Facet> facets, Iterable<Query> must, Iterable<Query> mustNot) {
		this.facets = ImmutableSet.copyOf(facets);
		this.must = ImmutableList.copyOf(must);
		this.mustNot = ImmutableList.copyOf(mustNot);
	}

	public ObjectNode execute(Index index) {
		SearchRequest request = buildSearch(index.getIndexName());
		SearchResponse<ObjectNode> response = index.search(request);
		return toJson(response);
	}

	public void execute(Index index, Callback<ObjectNode> callback) {
		index.find(buildQuery(), callback, 1000);
	}

	private SearchRequest buildSearch(String indexName) {
		SearchRequest.Builder builder = new SearchRequest.Builder();
		builder.index(indexName);
		builder.query(buildQuery());
		builder.size(0);
		for (Facet facet : facets) {
			facet.configure(builder);
		}
		return builder.build();
	}

	private Query buildQuery() {
		if (must.isEmpty() && mustNot.isEmpty()) {
			return Query.of(q -> q.matchAll(m -> m));
		}
		return Query.of(q -> q.bool(b -> {
			if (!must.isEmpty()) b.must(must);
			if (!mustNot.isEmpty()) b.mustNot(mustNot);
			return b;
		}));
	}

	private ObjectNode toJson(SearchResponse<ObjectNode> response) {
		ObjectNode node = Nodes.newObject();
		TOTAL.setValue(node, Ints.checkedCast(response.hits().total().value()));
		for (Facet facet : facets) {
			node.set(facet.getId(), facet.process(response));
		}
		return node;
	}

	@Override
	public String toString() {
		return String.format("Search(facets:%s, must:%s, must not:%s", facets, must, mustNot);
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Search &&
			equals((Search) that);
	}

	private boolean equals(Search that) {
		return facets.toString().equals(that.facets.toString()) &&
			toJsonStrings(must).equals(toJsonStrings(that.must)) &&
			toJsonStrings(mustNot).equals(toJsonStrings(that.mustNot));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(facets.toString(), toJsonStrings(must), toJsonStrings(mustNot));
	}

	private static String toJsonStrings(ImmutableList<Query> queries) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < queries.size(); i++) {
			if (i > 0) sb.append(",");
			sb.append(queries.get(i).toJsonString());
		}
		return sb.append("]").toString();
	}
}
