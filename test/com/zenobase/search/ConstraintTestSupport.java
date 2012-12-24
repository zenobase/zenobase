package com.zenobase.search;

public class ConstraintTestSupport extends SearchTestSupport {

	protected void addFilter(String expression, Object... args) {
		getSearch().addFilter(String.format(expression, args));
	}
}
