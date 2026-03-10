package com.zenobase.search;

import org.opensearch.client.opensearch._types.query_dsl.ExistsQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;

public class ExistsConstraintBuilder extends ConstraintBuilder {

	public ExistsConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public Query build(String value) {
		return "*".equals(value) ? ExistsQuery.of(e -> e.field(getPath()))._toQuery() : null;
	}
}
