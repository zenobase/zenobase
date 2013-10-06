package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;

public class ShadowFieldConstraintBuilder implements ConstraintBuilder {

	private final ConstraintBuilder target;

	public ShadowFieldConstraintBuilder(ConstraintBuilder target) {
		this.target = target;
	}

	@Override
	public QueryBuilder build(String field, String value) {
		if (field.contains(".")) {
			field = "$" + field;
		}
		return target.build(field, value);
	}
}
