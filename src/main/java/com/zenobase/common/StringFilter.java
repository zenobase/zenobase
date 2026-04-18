package com.zenobase.common;

public interface StringFilter {
	void put(String value);

	boolean mightContain(String value);
}
