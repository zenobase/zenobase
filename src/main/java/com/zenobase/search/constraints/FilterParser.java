package com.zenobase.search.constraints;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMultimap;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.query_dsl.Query;

public class FilterParser {

	private final ImmutableMultimap<String, ConstraintBuilder> constraintBuilders;

	public FilterParser(ImmutableMultimap<String, ConstraintBuilder> constraintBuilders) {
		this.constraintBuilders = constraintBuilders;
	}

	public @Nullable Query parse(@Nullable String value) {
		return !Strings.isNullOrEmpty(value)
			? new FilterBuilderSupport(constraintBuilders).addConstraints(value).buildFilter()
			: null;
	}
}
