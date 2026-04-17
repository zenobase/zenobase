package com.zenobase.search.facets;

import java.util.Map;

import com.google.common.collect.Maps;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class FacetOptions {

	private final Map<String, String> map;

	private FacetOptions() {
		this(Maps.newLinkedHashMap());
	}

	public FacetOptions(Map<String, String> map) {
		this.map = map;
	}

	public @Nullable String get(String key) {
		return get(key, String.class, null);
	}

	@SuppressWarnings("unchecked")
	public <T> @Nullable T get(String key, Class<T> type, @Nullable T defaultValue) {
		String value = map.get(key);
		if (value == null) {
			return defaultValue;
		}
		if (type.equals(String.class)) {
			return (T) value;
		}
		if (type.equals(Integer.class)) {
			return (T) Integer.valueOf(value);
		}
		if (type.equals(Long.class)) {
			return (T) Long.valueOf(value);
		}
		if (type.equals(Double.class)) {
			return (T) Double.valueOf(value);
		}
		if (type.equals(Boolean.class)) {
			return (T) Boolean.valueOf(value);
		}
		if (type.equals(DateTimeZone.class)) {
			return (T) DateTimeZone.forID(value);
		}
		throw new IllegalArgumentException("Don't know how to handle type <" + type + ">");
	}

	private void set(String key, String value) {
		map.put(key, value);
	}

	public static FacetOptions parse(String value) {
		var options = new FacetOptions();
		for (String option : value.split("(?<!\\\\),")) { // unescaped commas
			String[] tokens = option.replaceAll("\\\\,", ",").split(":", 2);
			if (tokens.length == 2 && !isEmpty(tokens[0]) && !isEmpty(tokens[1])) {
				options.set(tokens[0], tokens[1]);
			}
		}
		return options;
	}

	private static boolean isEmpty(String value) {
		return value.isBlank() || "null".equals(value);
	}
}
