package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

/**
 * Parses a response page from the Google Health {@code steps} resource. The {@code :dailyRollup} method returns
 * {@code StepsRollupValue} entries with {@code startTime}, {@code endTime} and {@code count}; {@code :list} returns
 * the same shape with sub-day ranges when {@code hourly} is set.
 */
class GoogleHealthStepsResult extends GoogleHealthResultSupport {

	GoogleHealthStepsResult(JsonNode node, @Nullable String tag, Identity author, DateTimeZone timezone) {
		super(node, tag, author, timezone);
	}

	List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode item : node.path("stepsRollupValues")) {
			int count = item.path("count").intValue();
			if (count <= 0) {
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
				event.setValue(Event.DURATION, new org.joda.time.Duration(begin, end));
			}
			event.setValue(Event.COUNT, count);
			event.setValue(Event.AUTHOR, author);
			setSources(event, item.path("origin"));
			events.add(event);
		}
		return events;
	}
}
