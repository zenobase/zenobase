package com.zenobase.common;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class SecureHash {

	private final List<String> values = Lists.newArrayList();

	public SecureHash add(String value) {
		values.add(value);
		return this;
	}

	public String build() {
		return Joiner.on('\t').join(values);
	}
}
