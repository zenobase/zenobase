package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

/**
 * Parses responses from {@code dailyOxygenSaturation:list}. Each entry has a {@code startTime} and an integer
 * {@code percent} in [0, 100].
 */
class GoogleHealthSpo2Result extends GoogleHealthResultSupport {

	GoogleHealthSpo2Result(JsonNode node, @Nullable String tag, Identity author, DateTimeZone timezone) {
		super(node, tag, author, timezone);
	}

	List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode item : node.path("dailyOxygenSaturationValues")) {
			int percent = item.path("percent").asInt();
			if (percent <= 0) {
				continue;
			}
			Event event = new Event();
			event.setValue(Event.TAG, tag != null ? tag : "spo2");
			event.setValue(Event.TIMESTAMP, dateTimeValue(item.path("startTime"), timezone));
			event.setValue(Event.PERCENTAGE, Percentage.valueOf(percent));
			event.setValue(Event.AUTHOR, author);
			setSources(event, item.path("origin"));
			events.add(event);
		}
		return events;
	}
}
