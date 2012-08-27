package com.zenobase.common;

import org.fest.assertions.Assertions;
import org.junit.Test;

public class GlobalsTest {

	@Test
	public void test() {
		Integer value = 42;
		Assertions.assertThat(Globals.get(Integer.class)).isNull();
		Globals.put(Integer.class, value);
		Assertions.assertThat(Globals.get(Integer.class)).isEqualTo(value);
	}
}
