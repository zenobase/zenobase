package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.joda.time.Duration;

import com.zenobase.common.DurationFormat;
import com.zenobase.json.Field;

public class DurationConstraintBuilder extends ConstraintBuilder {

	public DurationConstraintBuilder(Field<?> field) {
		super(field);
	}

	@Override
	public QueryBuilder build(String value) {
		return build(DurationFormat.parse(value));
	}

	private QueryBuilder build(Duration value) {
		return QueryBuilders.termQuery(getField().getPath(), value.getMillis());
	}
}
