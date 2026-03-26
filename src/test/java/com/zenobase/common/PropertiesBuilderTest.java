package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.Test;

public class PropertiesBuilderTest {

	@Test
	public void test() {

		String key = "me";
		String value = "too";
		Properties properties = new PropertiesBuilder().put(key, value).build();

		assertThat(properties.getProperty(key)).isEqualTo(value);
	}
}
