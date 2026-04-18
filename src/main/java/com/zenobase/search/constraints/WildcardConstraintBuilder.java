package com.zenobase.search.constraints;

import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.query_dsl.Query;

public class WildcardConstraintBuilder extends ConstraintBuilder {

	public WildcardConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public @Nullable Query build(String value) {
		return containsWildcard(value) ? Query.of(q -> q.wildcard(w -> w.field(getPath()).value(value))) : null;
	}

	private static boolean containsWildcard(String value) {
		return value.indexOf('*') != -1;
	}
}
