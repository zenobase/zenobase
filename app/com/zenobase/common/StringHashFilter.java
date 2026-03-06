package com.zenobase.common;

import java.util.HashSet;
import java.util.Set;

public class StringHashFilter implements StringFilter {

	private final Set<String> set = new HashSet<>();

	public void put(String value) {
		set.add(value);
	}

	public boolean mightContain(String value) {
		return set.contains(value);
	}
}
