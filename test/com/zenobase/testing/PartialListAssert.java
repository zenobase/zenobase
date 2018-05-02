package com.zenobase.testing;

import java.util.List;

import com.google.common.collect.ImmutableList;
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

	public PartialListAssert hasTotal(long expected) {
		Assertions.assertThat(actual.getTotal()).as("total").isEqualTo(expected);
		return this;
	}

	public PartialListAssert isEqualTo(List<?> expected) {
		Assertions.assertThat(ImmutableList.copyOf(actual)).as("elements").isEqualTo(expected);
		return this;
	}

	public PartialListAssert isEmpty() {
		Assertions.assertThat(actual).as("elements").isEmpty();
		return this;
	}
}
