package com.zenobase.tasks;

import com.google.common.util.concurrent.RateLimiter;

/** Rate limiters for external API calls; unlimited when {@code zenobase.rateLimits.disabled} is set. */
public final class RateLimiters {

	private static final boolean DISABLED = Boolean.getBoolean("zenobase.rateLimits.disabled");

	private RateLimiters() {}

	public static RateLimiter create(double permitsPerSecond) {
		return RateLimiter.create(DISABLED ? Double.MAX_VALUE : permitsPerSecond);
	}
}
