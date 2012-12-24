package com.zenobase.search;

public class ConstraintTestSupport extends SearchTestSupport {

	protected void addConstraint(String expression, Object... args) {
		getSearch().addConstraint(String.format(expression, args));
	}
}
