package com.zenobase.services;

import java.util.List;

import org.elasticsearch.common.collect.Iterables;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.zenobase.json.Field;

public class QuerySupport {

	private final List<QueryBuilder> constraints = Lists.newArrayList();

	protected QuerySupport() {

	}

	private void add(QueryBuilder constraint) {
		constraints.add(constraint);
	}

	protected QuerySupport equalTo(Field<?> field, Object value) {
		if (value != null) {
			add(QueryBuilders.termQuery(field.getName(), value));
		} else {
			isNull(field);
		}
		return this;
	}

	protected QuerySupport isNull(Field<?> field) {
		add(QueryBuilders.constantScoreQuery(FilterBuilders.missingFilter(field.getName())));
		return this;
	}

	protected QuerySupport notNull(Field<?> field) {
		add(QueryBuilders.constantScoreQuery(FilterBuilders.existsFilter(field.getName())));
		return this;
	}

	protected QuerySupport lessThan(Field<?> field, Object value) {
		add(QueryBuilders.rangeQuery(field.getName()).lt(value));
		return this;
	}

	public QueryBuilder build() {
		if (constraints.isEmpty()) {
			return QueryBuilders.matchAllQuery();
		}
		if (constraints.size() == 1) {
			return Iterables.getOnlyElement(constraints);
		}
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		for (QueryBuilder constraint : constraints) {
			query.must(constraint);
		}
		return query;
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof QuerySupport
			&& equals((QuerySupport) that);
	}

	private boolean equals(QuerySupport that) {
		return constraints.toString().equals(that.constraints.toString());
	}

	@Override
	public int hashCode() {
		return constraints.toString().hashCode();
	}
}
