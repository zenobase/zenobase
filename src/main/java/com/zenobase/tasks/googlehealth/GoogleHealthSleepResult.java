package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.jspecify.annotations.Nullable;

/**
 * Parses responses from {@code sleep:list}. Each entry is a sleep session carrying a {@code startTime},
 * {@code endTime}, and an optional {@code stages} array of {@code (stage, durationSeconds)} pairs.
 */
class GoogleHealthSleepResult extends GoogleHealthResultSupport {

	GoogleHealthSleepResult(JsonNode node, @Nullable String tag, Identity author, DateTimeZone timezone) {
		super(node, tag, author, timezone);
	}

	List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode session : node.path("sleepSessions")) {
			DateTime begin = dateTimeValue(session.path("startTime"), timezone);
			DateTime end = dateTimeValue(session.path("endTime"), timezone);
			Event event = new Event();
			if (tag != null) {
				event.setValue(Event.TAG, tag);
			}
			event.setValue(Event.TIMESTAMP, begin);
			event.addValue(Event.TIMESTAMP, end);
			event.setValue(Event.DURATION, new Duration(begin, end));
			event.setValue(Event.AUTHOR, author);
			setSources(event, session.path("origin"));
			// Stage totals are not represented as distinct fields on Event today; surface them in NOTE so they are
			// queryable until we add first-class sleep-stage fields to the data model.
			StringBuilder stages = new StringBuilder();
			for (JsonNode stage : session.path("stages")) {
				Duration d = durationValue(stage.path("duration"));
				if (d != null) {
					if (stages.length() > 0) {
						stages.append(", ");
					}
					stages.append(stage.path("stage").asText()).append(": ").append(d.getStandardMinutes()).append("m");
				}
			}
			if (stages.length() > 0) {
				event.setValue(Event.NOTE, stages.toString());
			}
			events.add(event);
		}
		return events;
	}
}
