package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import org.junit.jupiter.api.Test;

public class RandomElementTest {

	@Test
	public void test() {
		RandomElement<Character> rand = new RandomElement<>();
		rand.add('A', 4);
		rand.add('B', 2);
		rand.add('C', 1);
		Multiset<Character> found = TreeMultiset.create();
		for (int i = 0; i < 10000; ++i) {
			found.add(rand.next());
		}
		assertThat(found.count('A')).as("#A vs #B").isGreaterThan(found.count('B'));
		assertThat(found.count('B')).as("#B vs #C").isGreaterThan(found.count('C'));
	}
}
