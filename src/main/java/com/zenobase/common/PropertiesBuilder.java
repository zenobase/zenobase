package com.zenobase.common;

import java.util.Properties;

public class PropertiesBuilder {

	private final Properties properties = new Properties();

	public PropertiesBuilder put(String key, String value) {
		properties.put(key, value);
		return this;
	}

	public Properties build() {
		return new Properties(properties);
	}

}