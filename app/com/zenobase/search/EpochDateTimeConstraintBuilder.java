package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.zenobase.common.Characters;

public class EpochDateTimeConstraintBuilder implements ConstraintBuilder {

	@Override
	public QueryBuilder build(String field, String value) {
		return Characters.isDigits(value) ? QueryBuilders.termQuery(field, value) : null;
	}
}
