package com.zenobase.search;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMultimap;
import org.elasticsearch.index.query.FilterBuilder;

public class FilterParser {

	private final ImmutableMultimap<String, ConstraintBuilder> constraintBuilders;

	public FilterParser(ImmutableMultimap<String, ConstraintBuilder> constraintBuilders) {
		this.constraintBuilders = constraintBuilders;
	}

	public FilterBuilder parse(String value) {
		return !Strings.isNullOrEmpty(value) ? new FilterBuilderSupport(constraintBuilders).addConstraints(value).buildFilter() : null;
	}
}
