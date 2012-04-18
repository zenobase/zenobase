package com.zenobase.common;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

public class GeneratorTest {

	@Test
	public void test() {
		Multiset<Character> freq = TreeMultiset.create();
		for (int i = 0; i < 10000; ++i) {
			String id = Generator.id();
			Assert.assertEquals("Length of identifier: " + id, 10, id.length());
			freq.add(id.charAt(0));
		}
		for (Multiset.Entry<Character> entry : freq.entrySet()) {
			Assert.assertTrue("Improbable distribution of " + entry.getElement() + ": " + entry.getCount(),
				entry.getCount() > 250 && entry.getCount() < 400);
		}
	}
}
