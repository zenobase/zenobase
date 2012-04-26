package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import com.google.common.collect.Lists;

public class PartialListTest {

	@Test
	public void test() {

		List<String> values = Lists.newArrayList("a", "b", "c");
		PartialList<String> partial = new PartialList<String>(values, 100L);
		assertThat(partial.getElements()).as("elements").isEqualTo(values);
		assertThat(partial.size()).as("size").isEqualTo(100L);

		values.add("d");
		assertThat(partial.getElements()).as("original elements").isNotEqualTo(values);
	}
}
