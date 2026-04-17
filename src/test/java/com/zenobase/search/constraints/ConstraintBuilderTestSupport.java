package com.zenobase.search.constraints;

import com.zenobase.search.SearchTestSupport;

public class ConstraintBuilderTestSupport extends SearchTestSupport {

	protected void addConstraint(String expression, Object... args) {
		getSearch().addConstraint(String.format(expression, args));
	}
}
