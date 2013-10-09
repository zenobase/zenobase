package com.zenobase.search;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;

import com.zenobase.json.IntegerField;
import com.zenobase.json.Nodes;
import com.zenobase.services.Index;

public class Search {

	public static final IntegerField TOTAL = new IntegerField("total");

	private final ImmutableSet<Facet> facets;
	private final ImmutableList<QueryBuilder> must, mustNot;

	public Search(Iterable<Facet> facets, Iterable<QueryBuilder> must, Iterable<QueryBuilder> mustNot) {
		this.facets = ImmutableSet.copyOf(facets);
		this.must = ImmutableList.copyOf(must);
		this.mustNot = ImmutableList.copyOf(mustNot);
	}

	public ObjectNode execute(Index index) {
		SearchSourceBuilder builder = buildSearch();
		// Logger.info("q: " + builder);
		SearchResponse response = index.search(builder);
		// Logger.info("r: " + response);
		return toJson(response);
	}

	private SearchSourceBuilder buildSearch() {
		SearchSourceBuilder builder = new SearchSourceBuilder().query(buildQuery()).size(0);
		for (Facet facet : facets) {
			facet.configure(builder);
		}
		return builder;
	}

	private QueryBuilder buildQuery() {
		QueryBuilder query = null;
		if (must.isEmpty() && mustNot.isEmpty()) {
			query = QueryBuilders.matchAllQuery();
		} else {
			query = QueryBuilders.boolQuery();
			for (QueryBuilder constraint : must) {
				((BoolQueryBuilder) query).must(constraint);
			}
			for (QueryBuilder constraint : mustNot) {
				((BoolQueryBuilder) query).mustNot(constraint);
			}
		}
		return query;
	}

	private ObjectNode toJson(SearchResponse response) {
		ObjectNode node = Nodes.newObject();
		TOTAL.setValue(node, Ints.checkedCast(response.getHits().getTotalHits()));
		for (Facet facet : facets) {
			node.put(facet.getId(), facet.process(response));
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
			must.toString().equals(that.must.toString()) &&
			mustNot.toString().equals(that.mustNot.toString());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(facets.toString(), must.toString(), mustNot.toString());
	}
}
