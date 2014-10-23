package com.zenobase.tasks.fitbit;

import java.util.List;
import java.util.Map;

import org.elasticsearch.common.base.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class FitbitIntradayStepsResult {

	public static final Resource SOURCE = new Resource("Fitbit", "http://fitbit.com/");

	private final JsonNode node;
	private final String tag;
	private final Identity author;
	private final LocalDate date;
	private final DateTimeZone timezone;

	public FitbitIntradayStepsResult(JsonNode node, String tag, Identity author, LocalDate date, DateTimeZone timezone) {
		this.node = node;
		this.tag = tag;
		this.author = author;
		this.date = date;
		this.timezone = timezone;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (Map.Entry<DateTime, Integer> entry : valuesByHour().entrySet()) {
			events.add(toEvent(entry.getKey(), entry.getValue()));
		}
		return events;
	}

	private Map<DateTime, Integer> valuesByHour() {
		Map<DateTime, Integer> values = Maps.newLinkedHashMap();
		for (JsonNode recordNode : node.path("activities-steps-intraday").path("dataset")) {
			DateTime hour = toDateTimeFullHour(LocalTime.parse(recordNode.path("time").textValue()));
			int value = recordNode.path("value").intValue();
			Integer count = Objects.firstNonNull(values.get(hour), 0);
			values.put(hour, count + value);
		}
		return values;
	}

	private DateTime toDateTimeFullHour(LocalTime local) {
		return date.toDateTime(local, timezone).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
	}

	private Event toEvent(DateTime timestamp, int count) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, timestamp);
		event.setValue(Event.DURATION, Duration.standardHours(1));
		event.setValue(Event.COUNT, count);
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
