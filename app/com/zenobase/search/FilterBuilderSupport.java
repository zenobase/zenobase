package com.zenobase.search;

import java.util.List;

import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;

public class FilterBuilderSupport {

	private final ImmutableMultimap<String, ConstraintBuilder> constraintBuilders;
	private final List<QueryBuilder> must = Lists.newArrayList();
	private final List<QueryBuilder> mustNot = Lists.newArrayList();

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
		List<QueryBuilder> builders = Lists.newArrayList();
		for (String value : values) {
			for (ConstraintBuilder constraint : constraintBuilders.get(field)) {
				QueryBuilder builder = constraint.build(value);
				if (builder != null) {
					builders.add(builder);
					break;
				}
			}
		}
		if (builders.size() == 1) {
			return addConstraint(builders.get(0), negated);
		} else if (builders.size() > 1) {
			BoolQueryBuilder or = new BoolQueryBuilder();
			for (QueryBuilder builder : builders) {
				or.should(builder);
			}
			return addConstraint(or, negated);
		}
		throw new IllegalArgumentException("Don't know what to do with constraint: " + expression);
	}

	public FilterBuilderSupport addConstraint(QueryBuilder builder, boolean negated) {
		(negated ? mustNot : must).add(builder);
		return this;
	}

	protected List<QueryBuilder> getMust() {
		return must;
	}

	protected List<QueryBuilder> getMustNot() {
		return mustNot;
	}

	public FilterBuilder buildFilter() {
		FilterBuilder filter = null;
		if (must.isEmpty() && mustNot.isEmpty()) {
			filter = FilterBuilders.matchAllFilter();
		} else {
			filter = FilterBuilders.boolFilter();
			for (QueryBuilder constraint : must) {
				((BoolFilterBuilder) filter).must(FilterBuilders.queryFilter(constraint));
			}
			for (QueryBuilder constraint : mustNot) {
				((BoolFilterBuilder) filter).mustNot(FilterBuilders.queryFilter(constraint));
			}
		}
		return filter;
	}
}
