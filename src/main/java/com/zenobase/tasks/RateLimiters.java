package com.zenobase.tasks;

import com.google.common.util.concurrent.RateLimiter;

/**
 * Creates the rate limiters that throttle calls to external services. Setting the
 * {@code zenobase.rateLimits.disabled} system property (as the unit tests do) makes them unlimited.
 */
public final class RateLimiters {

	private static final boolean DISABLED = Boolean.getBoolean("zenobase.rateLimits.disabled");

	private RateLimiters() {}

	public static RateLimiter create(double permitsPerSecond) {
		return RateLimiter.create(DISABLED ? Double.MAX_VALUE : permitsPerSecond);
	}
}
