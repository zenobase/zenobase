package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;

import com.zenobase.json.Field;

public abstract class ConstraintBuilder {

	private final Field<?> field;

	protected ConstraintBuilder(Field<?> field) {
		this.field = field;
	}

	public Field<?> getField() {
		return field;
	}

	public abstract QueryBuilder build(String value);
}
