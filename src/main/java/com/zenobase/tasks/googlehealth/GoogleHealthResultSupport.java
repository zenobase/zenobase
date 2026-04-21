package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.format.ISODateTimeFormat;
import org.jspecify.annotations.Nullable;

/**
 * Shared helpers for parsing Google Health API response nodes. The API returns RFC3339 timestamps and a small set of
 * scalar shapes per data type; result subclasses extract units and magnitudes inline since each resource has its own
 * numeric field names (e.g. {@code kilocalories}, {@code kilograms}, {@code bpm}, {@code percent}).
 */
abstract class GoogleHealthResultSupport {

	/**
	 * Fallback source stamped on events when the response payload does not identify the originating device/app. If the
	 * Google Health response carries a device or application name we layer those on top — see {@link #setSources}.
	 */
	public static final Resource SOURCE = new Resource("Google Health", "https://health.google/");

	protected final JsonNode node;
	protected final @Nullable String tag;
	protected final Identity author;
	protected final DateTimeZone timezone;

	protected GoogleHealthResultSupport(JsonNode node, @Nullable String tag, Identity author, DateTimeZone timezone) {
		this.node = node;
		this.tag = tag;
		this.author = author;
		this.timezone = timezone;
	}

	protected static DateTime dateTimeValue(JsonNode item, DateTimeZone zone) {
		return ISODateTimeFormat.dateTimeParser().withOffsetParsed().parseDateTime(item.textValue()).withZone(zone);
	}

	/**
	 * Parse a protobuf-style duration node. Accepts a number of milliseconds or a string ending in {@code "s"} with
	 * fractional seconds (e.g. {@code "90.5s"}).
	 */
	protected static @Nullable Duration durationValue(JsonNode node) {
		if (node.isMissingNode() || node.isNull()) {
			return null;
		}
		if (node.isNumber()) {
			return Duration.millis(node.longValue());
		}
		String text = node.textValue();
		if (text == null || text.isEmpty()) {
			return null;
		}
		Preconditions.checkArgument(text.endsWith("s"), "unexpected duration format: %s", text);
		double seconds = Double.parseDouble(text.substring(0, text.length() - 1));
		return seconds > 0.0 ? Duration.millis(Math.round(seconds * 1000.0)) : null;
	}

	/** The {@code nextPageToken} off the root response node, or null when the page is the last one. */
	@Nullable
	final String getNextPageToken() {
		String token = node.path("nextPageToken").textValue();
		return token != null && !token.isEmpty() ? token : null;
	}

	/**
	 * Stamp {@link Event#SOURCE} with a layered provenance chain, always including the "Google Health" umbrella and
	 * adding the application and device names when the origin node exposes them. The order is most-specific-last so a
	 * consumer reading just the first value still sees "Google Health" at minimum.
	 */
	protected static void setSources(Event event, @Nullable JsonNode origin) {
		for (Resource source : resolveSources(origin)) {
			event.addValue(Event.SOURCE, source);
		}
	}

	private static ImmutableList<Resource> resolveSources(@Nullable JsonNode origin) {
		ImmutableList.Builder<Resource> sources = ImmutableList.builder();
		sources.add(SOURCE);
		if (origin != null && !origin.isMissingNode() && !origin.isNull()) {
			String application = origin.path("applicationName").textValue();
			if (application != null && !application.isEmpty()) {
				sources.add(Sources.resolve(application));
			}
			String device = origin.path("device").path("model").textValue();
			if (device != null && !device.isEmpty()) {
				sources.add(Sources.resolve(device));
			}
		}
		return sources.build();
	}
}
