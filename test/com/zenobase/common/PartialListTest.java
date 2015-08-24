package com.zenobase.common;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;
import org.junit.Test;

public class PartialListTest {

	@Test
	public void testEqualsHashCode() {

		List<String> alphabet = Lists.newArrayList("a", "b", "c");
		List<String> numbers = Lists.newArrayList("1", "2", "3");

		new EqualsTester()
			.addEqualityGroup(new DefaultPartialList<>(alphabet, 100), new DefaultPartialList<>(alphabet, 100))
			.addEqualityGroup(new DefaultPartialList<>(alphabet, 200))
			.addEqualityGroup(new DefaultPartialList<>(numbers, 100))
			.testEquals();
	}
}
