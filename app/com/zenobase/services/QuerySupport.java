package com.zenobase.services;

import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.ExistsQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchAllQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryStringQuery;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;
import org.opensearch.client.opensearch._types.FieldValue;

import com.zenobase.json.Field;

public class QuerySupport {

	private final List<Query> constraints = Lists.newArrayList();

	protected QuerySupport() {

	}

	protected QuerySupport equalTo(Field<?> field, Object value) {
		if (value != null) {
			add(TermQuery.of(t -> t.field(field.getName()).value(toFieldValue(value)))._toQuery());
		} else {
			isNull(field);
		}
		return this;
	}

	protected QuerySupport notEqualTo(Field<?> field, Object value) {
		add(BoolQuery.of(b -> b.mustNot(TermQuery.of(t -> t.field(field.getName()).value(toFieldValue(value)))._toQuery()))._toQuery());
		return this;
	}

	protected QuerySupport isNull(Field<?> field) {
		add(BoolQuery.of(b -> b.mustNot(ExistsQuery.of(e -> e.field(field.getName()))._toQuery()))._toQuery());
		return this;
	}

	protected QuerySupport notNull(Field<?> field) {
		add(ExistsQuery.of(e -> e.field(field.getName()))._toQuery());
		return this;
	}

	protected QuerySupport lessThan(Field<?> field, Object value) {
		Object jsonValue = value instanceof org.joda.time.DateTime ? value.toString() : value;
		add(RangeQuery.of(r -> r.field(field.getName()).lt(org.opensearch.client.json.JsonData.of(jsonValue)))._toQuery());
		return this;
	}

	protected QuerySupport queryString(String query, String defaultField) {
		add(QueryStringQuery.of(qs -> qs.query(query).defaultField(defaultField))._toQuery());
		return this;
	}

	protected void add(Query constraint) {
		constraints.add(constraint);
	}

	public Query build() {
		if (constraints.isEmpty()) {
			return MatchAllQuery.of(m -> m)._toQuery();
		}
		if (constraints.size() == 1) {
			return Iterables.getOnlyElement(constraints);
		}
		return BoolQuery.of(b -> b.must(constraints))._toQuery();
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof QuerySupport
			&& equals((QuerySupport) that);
	}

	private boolean equals(QuerySupport that) {
		return toJsonStrings(constraints).equals(toJsonStrings(that.constraints));
	}

	@Override
	public int hashCode() {
		return toJsonStrings(constraints).hashCode();
	}

	private static String toJsonStrings(List<Query> queries) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < queries.size(); i++) {
			if (i > 0) sb.append(",");
			sb.append(queries.get(i).toJsonString());
		}
		return sb.append("]").toString();
	}

	static FieldValue toFieldValue(Object value) {
		if (value instanceof String) {
			return FieldValue.of((String) value);
		} else if (value instanceof Boolean) {
			return FieldValue.of((Boolean) value);
		} else if (value instanceof Long) {
			return FieldValue.of((Long) value);
		} else if (value instanceof Integer) {
			return FieldValue.of((long)(Integer) value);
		} else if (value instanceof Double) {
			return FieldValue.of((Double) value);
		} else {
			return FieldValue.of(value.toString());
		}
	}

}
