package search;

import java.util.Map;

import org.elasticsearch.common.collect.Maps;
import org.joda.time.DateTimeZone;

public class WidgetOptions {

	private final Map<String, String> map = Maps.newHashMap();

	public String get(String key) {
		return get(key, String.class, null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> T get(String key, Class<T> type, T defaultValue) {
		String value = map.get(key);
		if (value == null) {
			return defaultValue;
		}
		if (type.equals(String.class)) {
			return (T) String.valueOf(value);
		}
		if (type.equals(Integer.class)) {
			return (T) Integer.valueOf(value);
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
		if (type.isEnum()) {
			return (T) Enum.valueOf((Class<? extends Enum>) type, value);
		}
		throw new UnsupportedOperationException("Don't know how to handle " + type);
	}

	private void set(String key, String value) {
		map.put(key, value);
	}

	public static WidgetOptions parse(String value) {
		WidgetOptions options = new WidgetOptions();
		for (String option : value.split(",")) {
			String[] tokens = option.split(":", 2);
			if (tokens.length == 2) {
				options.set(tokens[0], tokens[1]);
			}
		}
		return options;
	}
}
