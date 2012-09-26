package com.zenobase.common;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

public class StringBloomFilter {

	private final BloomFilter<CharSequence> filter;

	public StringBloomFilter(int expectedSize) {
		filter = BloomFilter.create(Funnels.stringFunnel(), expectedSize);
	}

	public void put(String value) {
		filter.put(value);
	}

	public boolean mightContain(String value) {
		return filter.mightContain(value);
	}
}
