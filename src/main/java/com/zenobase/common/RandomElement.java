package com.zenobase.common;

import java.util.Objects;
import java.util.Random;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

public class RandomElement<T> {

	private final Random rand = new Random();
	private final Multiset<T> elements = LinkedHashMultiset.create();

	public RandomElement<T> add(T element, int weight) {
		elements.add(element, weight);
		return this;
	}

	public T next() {
		int r = rand.nextInt(elements.size() + 1);
		int sum = 0;
		for (Multiset.Entry<T> entry : elements.entrySet()) {
			if ((sum += entry.getCount()) >= r) {
				return Objects.requireNonNull(entry.getElement());
			}
		}
		throw new AssertionError();
	}
}
