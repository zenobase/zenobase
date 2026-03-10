package com.zenobase.search;

import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.joda.time.Duration;

import com.zenobase.common.DurationFormat;

public class DurationConstraintBuilder extends ConstraintBuilder {

	public DurationConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public QueryBuilder build(String value) {
		return build(DurationFormat.parse(value));
	}

	private QueryBuilder build(Duration value) {
		return QueryBuilders.termQuery(getPath(), value.getMillis());
	}
}
