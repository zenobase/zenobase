package com.zenobase.search;

import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.WildcardQuery;

public class WildcardConstraintBuilder extends ConstraintBuilder {

	public WildcardConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public Query build(String value) {
		return containsWildcard(value) ? WildcardQuery.of(w -> w.field(getPath()).value(value))._toQuery() : null;
	}

	private static boolean containsWildcard(String value) {
		return value.indexOf('*') != -1;
	}
}
