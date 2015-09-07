package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.zenobase.common.Characters;

public class EpochDateTimeConstraintBuilder extends ConstraintBuilder {

	public EpochDateTimeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public QueryBuilder build(String value) {
		return Characters.isDigits(value) && value.length() > 4 ? QueryBuilders.matchQuery(getPath(), Long.parseLong(value)) : null;
	}
}
