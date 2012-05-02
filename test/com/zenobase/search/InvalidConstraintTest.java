package com.zenobase.search;

import org.junit.Test;

public class InvalidConstraintTest extends SearchTestSupport {

	@Test(expected = IllegalArgumentException.class)
	public void test() {

		addFilter("xxx:lunch");

		execute();
	}
}
