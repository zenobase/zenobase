package com.zenobase.testing;

import java.util.List;

import org.fest.assertions.Assertions;
import org.fest.assertions.GenericAssert;

import com.zenobase.common.PartialList;

public class PartialListAssert extends GenericAssert<PartialListAssert, PartialList<?>> {

	private PartialListAssert(PartialList<?> actual) {
		super(PartialListAssert.class, actual);
	}

	public static PartialListAssert assertThat(PartialList<?> actual) {
		return new PartialListAssert(actual);
	}

	public PartialListAssert hasSize(long expected) {
		Assertions.assertThat(actual.size()).as("size").isEqualTo(expected);
		return this;
	}

	public PartialListAssert isEqualTo(List<?> expected) {
		Assertions.assertThat(actual.getElements()).as("elements").isEqualTo(expected);
		return this;
	}
}
