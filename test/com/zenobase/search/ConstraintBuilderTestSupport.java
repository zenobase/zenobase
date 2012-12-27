package com.zenobase.search;

public class ConstraintBuilderTestSupport extends SearchTestSupport {

	protected void addConstraint(String expression, Object... args) {
		getSearch().addConstraint(String.format(expression, args));
	}
}
