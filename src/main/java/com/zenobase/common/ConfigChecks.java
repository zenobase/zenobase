package com.zenobase.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Up-front validation for required configuration values, so a missing or malformed setting fails at startup with a
 * message naming the key, rather than as an obscure error later.
 */
public final class ConfigChecks {

	private static final String HINT = "set it in the app config; see conf/application-local.yaml.template";

	private ConfigChecks() {}

	/** Returns {@code value} if it is non-blank, otherwise throws naming {@code key}. */
	public static String requireNonBlank(String key, String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(key + " is not configured (" + HINT + ")");
		}
		return value;
	}

	/** Returns {@code value} if it is an absolute http(s) URL with a host, otherwise throws naming {@code key}. */
	public static String requireHttpUrl(String key, String value) {
		requireNonBlank(key, value);
		URI uri;
		try {
			uri = new URI(value.strip());
		} catch (URISyntaxException e) {
			throw new IllegalStateException(key + " is not a valid URL: '" + value + "'", e);
		}
		String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
		if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null) {
			throw new IllegalStateException(
				key + " must be an absolute http(s) URL such as https://example.com, but was '" + value + "'"
			);
		}
		return value;
	}
}
