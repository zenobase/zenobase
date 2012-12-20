package com.zenobase.common;

import java.util.List;

import org.junit.Test;
import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;

public class PartialListTest {

	@Test
	public void testEqualsHashCode() {

		List<String> alphabet = Lists.newArrayList("a", "b", "c");
		List<String> numbers = Lists.newArrayList("1", "2", "3");

		new EqualsTester()
			.addEqualityGroup(new DefaultPartialList<String>(alphabet, 100), new DefaultPartialList<String>(alphabet, 100))
			.addEqualityGroup(new DefaultPartialList<String>(alphabet, 200))
			.addEqualityGroup(new DefaultPartialList<String>(numbers, 100))
			.testEquals();
	}
}
