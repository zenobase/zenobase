package com.zenobase.tasks.fitbit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class FitbitIntradayStepsResult extends FitbitResultSupport {

	private final LocalDate date;

	public FitbitIntradayStepsResult(
			JsonNode node, String tag, Identity author, LocalDate date, DateTimeZone timezone) {
		super(node, tag, author, timezone);
		this.date = date;
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (Map.Entry<DateTime, Integer> entry : valuesByHour().entrySet()) {
			events.add(toEvent(entry.getKey(), entry.getValue()));
		}
		return events;
	}

	private Map<DateTime, Integer> valuesByHour() {
		Map<DateTime, Integer> values = Maps.newLinkedHashMap();
		for (JsonNode recordNode : node.path("activities-steps-intraday").path("dataset")) {
			DateTime hour =
					toDateTimeFullHour(LocalTime.parse(recordNode.path("time").textValue()));
			if (hour != null) {
				int value = recordNode.path("value").intValue();
				Integer count = MoreObjects.firstNonNull(values.get(hour), 0);
				values.put(hour, count + value);
			}
		}
		return values;
	}

	private @Nullable DateTime toDateTimeFullHour(LocalTime local) {
		return toDateTimeFullHour(date.toLocalDateTime(local)
				.withMinuteOfHour(0)
				.withSecondOfMinute(0)
				.withMillisOfSecond(0));
	}

	private @Nullable DateTime toDateTimeFullHour(LocalDateTime local) {
		var tz = Objects.requireNonNull(timezone);
		return !tz.isLocalDateTimeGap(local) ? local.toDateTime(tz) : null;
	}

	private Event toEvent(DateTime timestamp, int count) {
		var event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, timestamp);
		event.setValue(Event.DURATION, Duration.standardHours(1));
		event.setValue(Event.COUNT, count);
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
