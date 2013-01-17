package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;
import org.junit.Test;

public class CharactersTest {

	@Test
	public void testDigits() {
		assertThat(Characters.isDigits("")).isFalse();
		assertThat(Characters.isDigits("1")).isTrue();
		assertThat(Characters.isDigits("1a")).isFalse();
		assertThat(Characters.isDigits("123")).isTrue();
	}
}
