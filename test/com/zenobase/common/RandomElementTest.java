package com.zenobase.common;

import org.junit.Assert;
import org.junit.Test;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

public class RandomElementTest {

	@Test
	public void test() {
		RandomElement<Character> rand = new RandomElement<Character>();
		rand.add('A', 4);
		rand.add('B', 2);
		rand.add('C', 1);
		Multiset<Character> found = TreeMultiset.create();
		for (int i = 0; i < 10000; ++i) {
			found.add(rand.next());
		}
		Assert.assertTrue("Expected more As than Bs: " + found, found.count('A') > found.count('B'));
		Assert.assertTrue("Expected more Bs than Cs: " + found, found.count('B') > found.count('C'));
	}
}
