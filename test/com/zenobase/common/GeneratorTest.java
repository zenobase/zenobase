package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import org.junit.Test;

public class GeneratorTest {

	@Test
	public void test() {
		Multiset<Character> freq = TreeMultiset.create();
		for (int i = 0; i < 10000; ++i) {
			String id = Generator.id();
			assertThat(id.length()).as("length of identifier " + id).isEqualTo(10);
			freq.add(id.charAt(0));
		}
		for (Multiset.Entry<Character> entry : freq.entrySet()) {
			assertThat(entry.getCount()).as("frequency of " + entry.getElement())
				.isGreaterThan(200).isLessThan(400);
		}
	}
}
