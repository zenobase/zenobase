package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.jspecify.annotations.Nullable;

/**
 * Parses responses from {@code heartRate:list}, {@code heartRate:dailyRollup}, and {@code dailyRestingHeartRate:list}.
 * Each entry carries a {@code startTime}, {@code endTime}, and a {@code bpm} numeric value.
 */
class GoogleHealthCardioResult extends GoogleHealthResultSupport {

	GoogleHealthCardioResult(JsonNode node, @Nullable String tag, Identity author, DateTimeZone timezone) {
		super(node, tag, author, timezone);
	}

	List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		// API may return either "heartRateRollupValues" or "dailyRestingHeartRateValues"; iterate any array child.
		JsonNode items = firstArrayChild();
		for (JsonNode item : items) {
			int bpm = item.path("bpm").asInt();
			if (bpm <= 0) {
				continue;
			}
			DateTime begin = dateTimeValue(item.path("startTime"), timezone);
			DateTime end = dateTimeValue(item.path("endTime"), timezone);
			Event event = new Event();
			if (tag != null) {
				event.setValue(Event.TAG, tag);
			}
			event.setValue(Event.TIMESTAMP, begin);
			if (!end.equals(begin)) {
				event.addValue(Event.TIMESTAMP, end);
				event.setValue(Event.DURATION, new Duration(begin, end));
			}
			event.setValue(Event.FREQUENCY, Measures.valueOf(BigDecimal.valueOf(bpm), Units.BPM));
			event.setValue(Event.AUTHOR, author);
			setSources(event, item.path("origin"));
			events.add(event);
		}
		return events;
	}

	private JsonNode firstArrayChild() {
		var it = node.fields();
		while (it.hasNext()) {
			var entry = it.next();
			if (entry.getValue().isArray()) {
				return entry.getValue();
			}
		}
		return node.path("values");
	}
}
