package com.zenobase.search;

import org.elasticsearch.index.query.FilterBuilder;
import com.google.common.collect.ImmutableMultimap;

public class FilterParser {

	private final ImmutableMultimap<String, ConstraintBuilder> constraintBuilders;

	public FilterParser(ImmutableMultimap<String, ConstraintBuilder> constraintBuilders) {
		this.constraintBuilders = constraintBuilders;
	}

	public FilterBuilder parse(String value) {
		return new FilterBuilderSupport(constraintBuilders).addConstraints(value).buildFilter();
	}
}
