package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.jspecify.annotations.Nullable;

/**
 * Parses responses from {@code dailyHeartRateVariability:list}. Each entry carries a {@code startTime} and an RMSSD
 * value in milliseconds.
 */
class GoogleHealthHrvResult extends GoogleHealthResultSupport {

	GoogleHealthHrvResult(JsonNode node, @Nullable String tag, Identity author, DateTimeZone timezone) {
		super(node, tag, author, timezone);
	}

	List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode item : node.path("dailyHeartRateVariabilityValues")) {
			long ms = item.path("rmssdMilliseconds").asLong();
			if (ms <= 0) {
				continue;
			}
			Event event = new Event();
			event.setValue(Event.TAG, tag != null ? tag : "hrv");
			event.setValue(Event.TIMESTAMP, dateTimeValue(item.path("startTime"), timezone));
			// HRV is represented as a DURATION (RMSSD ms) until a dedicated time-variability field exists.
			event.setValue(Event.DURATION, Duration.millis(ms));
			event.setValue(Event.AUTHOR, author);
			setSources(event, item.path("origin"));
			events.add(event);
		}
		return events;
	}
}
