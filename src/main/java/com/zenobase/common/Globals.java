package com.zenobase.common;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import org.jspecify.annotations.Nullable;

public class Globals {

	public static final ClassToInstanceMap<Object> instances = MutableClassToInstanceMap.create();

	private Globals() {}

	public static synchronized <T> void put(Class<T> type, T object) {
		instances.putInstance(type, object);
	}

	public static synchronized <T> @Nullable T get(Class<T> type) {
		return instances.getInstance(type);
	}
}
