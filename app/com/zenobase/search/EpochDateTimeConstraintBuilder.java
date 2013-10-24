package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.zenobase.common.Characters;
import com.zenobase.json.Field;

public class EpochDateTimeConstraintBuilder extends ConstraintBuilder {

	public EpochDateTimeConstraintBuilder(Field<?> field) {
		super(field);
	}

	@Override
	public QueryBuilder build(String value) {
		return Characters.isDigits(value) ? QueryBuilders.termQuery(getField().getPath(), value) : null;
	}
}
