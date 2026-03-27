package com.zenobase.search;

import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.query_dsl.Query;

public class PercentRangeConstraintBuilder extends DecimalRangeConstraintBuilder {

	public PercentRangeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public @Nullable Query build(String value) {
		return super.build(value.replace("%", ""));
	}
}
