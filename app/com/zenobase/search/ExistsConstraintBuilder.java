package com.zenobase.search;

import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class ExistsConstraintBuilder extends ConstraintBuilder {

	public ExistsConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public QueryBuilder build(String value) {
		return "*".equals(value) ? QueryBuilders.constantScoreQuery(FilterBuilders.existsFilter(getPath())) : null;
	}
}
