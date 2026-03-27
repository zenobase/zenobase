package com.zenobase.common;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

public class StringBloomFilter implements StringFilter {

	private final BloomFilter<CharSequence> filter;

	public StringBloomFilter(int expectedSize) {
		filter = BloomFilter.create(Funnels.unencodedCharsFunnel(), expectedSize, 0.001);
	}

	@Override
	public void put(String value) {
		filter.put(value);
	}

	@Override
	public boolean mightContain(String value) {
		return filter.mightContain(value);
	}
}
