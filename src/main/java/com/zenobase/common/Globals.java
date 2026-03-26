package com.zenobase.common;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

public class Globals {

	public static ClassToInstanceMap<Object> instances = MutableClassToInstanceMap.create();

	private Globals() {}

	public static synchronized <T> void put(Class<T> type, T object) {
		instances.putInstance(type, object);
	}

	public static synchronized <T> T get(Class<T> type) {
		return instances.getInstance(type);
	}
}
