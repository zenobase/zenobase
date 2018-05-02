package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Properties;

import org.junit.Test;

public class PropertiesBuilderTest {

	@Test
	public void test() {

		String key = "me";
		String value = "too";
		Properties properties = new PropertiesBuilder().put(key, value).build();

		assertThat(properties.getProperty(key)).isEqualTo(value);
	}
}
