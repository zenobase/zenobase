package com.zenobase.testing;

import com.google.common.collect.ImmutableList;
import com.zenobase.common.PartialList;
import java.util.List;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class PartialListAssert extends AbstractAssert<PartialListAssert, PartialList<?>> {

	private PartialListAssert(PartialList<?> actual) {
		super(actual, PartialListAssert.class);
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
