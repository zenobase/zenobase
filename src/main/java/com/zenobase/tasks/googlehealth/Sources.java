package com.zenobase.tasks.googlehealth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.zenobase.models.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Looks up a {@link Resource} for a Google Health application or device name. Names seen in real payloads are curated
 * into the {@link #KNOWN} map so events get stamped with the vendor's canonical URL; unknown names fall back to the
 * generic Google Health URL and trigger a one-time warning so new integrations can be added to the map.
 *
 * <p>The {@code applicationName} and {@code device.model} fields share this lookup since, in practice, the same vendor
 * (e.g. "Fitbit") appears in both shapes and we want a single source of truth.
 */
final class Sources {

	private static final Logger logger = LoggerFactory.getLogger(Sources.class);

	private static final String DEFAULT_URL = "https://health.google/";

	/**
	 * Known application and device names seen in the Google Health API. Seed this with new entries as real payloads
	 * surface them — the {@link #UNKNOWN} cache logs any miss so missing integrations are visible in the logs.
	 */
	private static final ImmutableMap<String, String> KNOWN = ImmutableMap.<String, String>builder().build();

	/** Names that have already been reported as unknown, so we only log one warning per name per process. */
	private static final Cache<String, Boolean> UNKNOWN = CacheBuilder.newBuilder().maximumSize(500).build();

	private Sources() {}

	/** Resolve a {@link Resource} for a display name, consulting {@link #KNOWN} first and falling back otherwise. */
	static Resource resolve(String name) {
		String url = KNOWN.get(name);
		if (url != null) {
			return new Resource(name, url);
		}
		if (UNKNOWN.getIfPresent(name) == null) {
			logger.warn("Unknown Google Health source: {}", name);
			UNKNOWN.put(name, Boolean.TRUE);
		}
		return new Resource(name, DEFAULT_URL);
	}
}
