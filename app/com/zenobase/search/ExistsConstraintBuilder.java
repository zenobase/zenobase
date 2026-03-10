package com.zenobase.search;

import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

public class ExistsConstraintBuilder extends ConstraintBuilder {

	public ExistsConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public QueryBuilder build(String value) {
		return "*".equals(value) ? QueryBuilders.existsQuery(getPath()) : null;
	}
}
