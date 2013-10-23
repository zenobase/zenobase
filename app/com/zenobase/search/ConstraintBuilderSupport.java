package com.zenobase.search;

public abstract class ConstraintBuilderSupport implements ConstraintBuilder {

	private final String field;

	protected ConstraintBuilderSupport(String field) {
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
