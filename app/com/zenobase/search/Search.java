package com.zenobase.search;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;

import com.zenobase.json.IntegerField;
import com.zenobase.json.Nodes;
import com.zenobase.services.Index;

public class Search {

	public static final IntegerField TOTAL = new IntegerField("total");

	private final ImmutableSet<Widget> widgets;
	private final ImmutableList<QueryBuilder> constraints;

	public Search(Iterable<Widget> widgets, Iterable<QueryBuilder> constraints) {
		this.widgets = ImmutableSet.copyOf(widgets);
		this.constraints = ImmutableList.copyOf(constraints);
	}

	public ObjectNode execute(Index index) {
		SearchSourceBuilder builder = buildSearch();
		// Logger.info("q: " + builder);
		SearchResponse response = index.search(builder);
		// Logger.info("r: " + response);
		return toJson(response);
	}

	private SearchSourceBuilder buildSearch() {
		SearchSourceBuilder builder = new SearchSourceBuilder().query(buildQuery());
		for (Widget widget : widgets) {
			widget.configure(builder);
		}
		return builder;
	}

	private QueryBuilder buildQuery() {
		QueryBuilder query = null;
		if (constraints.isEmpty()) {
			query = QueryBuilders.matchAllQuery();
		} else {
			query = QueryBuilders.boolQuery();
			for (QueryBuilder constraint : constraints) {
				((BoolQueryBuilder) query).must(constraint);
			}
		}
		return query;
	}

	private ObjectNode toJson(SearchResponse response) {
		ObjectNode node = Nodes.newObject();
		TOTAL.setValue(node, Ints.checkedCast(response.hits().getTotalHits()));
		for (Widget widget : widgets) {
			node.put(widget.getId(), widget.process(response));
		}
		return node;
	}

	@Override
	public String toString() {
		return String.format("Search(widgets:%s, constraints:%s", widgets, constraints);
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Search &&
			equals((Search) that);
	}

	private boolean equals(Search that) {
		return widgets.toString().equals(that.widgets.toString()) &&
			constraints.toString().equals(that.constraints.toString());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(widgets.toString(), constraints.toString());
	}
}
