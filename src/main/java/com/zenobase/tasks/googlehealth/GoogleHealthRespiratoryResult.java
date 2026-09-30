package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

/**
 * Parses responses from {@code respiratoryRate:list}. Each entry has a {@code startTime} and a breaths-per-minute
 * value. Stored as {@link Event#FREQUENCY} in {@link Units#BPM} — the tag distinguishes it from heart-rate events.
 */
class GoogleHealthRespiratoryResult extends GoogleHealthResultSupport {

	GoogleHealthRespiratoryResult(JsonNode node, @Nullable String tag, Identity author, DateTimeZone timezone) {
		super(node, tag, author, timezone);
	}

	List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode item : node.path("respiratoryRateValues")) {
			int rate = item.path("breathsPerMinute").asInt();
			if (rate <= 0) {
				continue;
			}
			Event event = new Event();
			event.setValue(Event.TAG, tag != null ? tag : "respiration");
			event.setValue(Event.TIMESTAMP, dateTimeValue(item.path("startTime"), timezone));
			event.setValue(Event.FREQUENCY, Measures.valueOf(BigDecimal.valueOf(rate), Units.BPM));
			event.setValue(Event.AUTHOR, author);
			setSources(event, item.path("origin"));
			events.add(event);
		}
		return events;
	}
}
