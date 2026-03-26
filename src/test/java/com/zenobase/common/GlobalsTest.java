package com.zenobase.common;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class GlobalsTest {

	@Test
	public void test() {
		Integer value = 42;
		Assertions.assertThat(Globals.get(Integer.class)).isNull();
		Globals.put(Integer.class, value);
		Assertions.assertThat(Globals.get(Integer.class)).isEqualTo(value);
	}
}
