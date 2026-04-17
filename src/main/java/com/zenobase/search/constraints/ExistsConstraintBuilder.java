package com.zenobase.search.constraints;

import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.query_dsl.Query;

public class ExistsConstraintBuilder extends ConstraintBuilder {

	public ExistsConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public @Nullable Query build(String value) {
		return "*".equals(value) ? Query.of(q -> q.exists(e -> e.field(getPath()))) : null;
	}
}
