package com.zenobase.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Urls {

	private static final Pattern PARAM = Pattern.compile("([?&])([^=&#]+)=([^&#]*)");
	private static final Pattern SENSITIVE = Pattern.compile(
		"token|secret|key|sig|password|code|verifier|^sk$",
		Pattern.CASE_INSENSITIVE
	);

	private Urls() {}

	/** Replaces the values of query parameters that may hold credentials, so the URL is safe to log. */
	public static String redact(String url) {
		return PARAM.matcher(url).replaceAll(m ->
			Matcher.quoteReplacement(
				SENSITIVE.matcher(m.group(2)).find() ? m.group(1) + m.group(2) + "=REDACTED" : m.group()
			)
		);
	}
}
