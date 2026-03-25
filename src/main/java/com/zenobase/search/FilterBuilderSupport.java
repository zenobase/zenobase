package com.zenobase.search;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.util.ArrayList;

import com.google.common.collect.ImmutableMultimap;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchAllQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;

public class FilterBuilderSupport {

	private final ImmutableMultimap<String, ConstraintBuilder> constraintBuilders;
	private final List<Query> must = new ArrayList<>();
	private final List<Query> mustNot = new ArrayList<>();

	public FilterBuilderSupport(ImmutableMultimap<String, ConstraintBuilder> constraintBuilders) {
		this.constraintBuilders = constraintBuilders;
	}

	public FilterBuilderSupport addConstraints(Iterable<String> expressions) {
		for (String expression : expressions) {
			addConstraint(expression);
		}
		return this;
	}

	public FilterBuilderSupport addConstraints(String expressions) {
		if (!Strings.isNullOrEmpty(expressions)) {
			for (String expression : Splitter.on('|').split(expressions)) {
				addConstraint(expression);
			}
		}
		return this;
	}

	public FilterBuilderSupport addConstraint(String expression) {
		String[] tokens = expression.split(":", 2);
		Preconditions.checkArgument(tokens.length == 2, "Can't parse constraint: " + expression);
		String field = tokens[0];
		String[] values = tokens[1].split(" OR ");
		boolean negated = false;
		if (field.startsWith("-")) {
			negated = true;
			field = field.substring(1);
		}
		List<Query> builders = new ArrayList<>();
		for (String value : values) {
			for (ConstraintBuilder constraint : constraintBuilders.get(field)) {
				Query builder = constraint.build(value);
				if (builder != null) {
					builders.add(builder);
					break;
				}
			}
		}
		if (builders.size() == 1) {
			return addConstraint(builders.get(0), negated);
		} else if (builders.size() > 1) {
			Query or = Query.of(q -> q.bool(b -> b.should(builders)));
			return addConstraint(or, negated);
		}
		throw new IllegalArgumentException("Don't know what to do with constraint: " + expression);
	}

	public FilterBuilderSupport addConstraint(Query builder, boolean negated) {
		(negated ? mustNot : must).add(builder);
		return this;
	}

	protected List<Query> getMust() {
		return must;
	}

	protected List<Query> getMustNot() {
		return mustNot;
	}

	public Query buildFilter() {
		if (must.isEmpty() && mustNot.isEmpty()) {
			return Query.of(q -> q.matchAll(m -> m));
		}
		return Query.of(q -> q.bool(b -> {
			if (!must.isEmpty()) b.must(must);
			if (!mustNot.isEmpty()) b.mustNot(mustNot);
			return b;
		}));
	}
}
