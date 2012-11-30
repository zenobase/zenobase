package com.zenobase.tasks;

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.elasticsearch.common.collect.Lists;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Rating;

class FitbitSleepResult {

	private final JsonNode node;
	private final Identity author;
	private final DateTimeZone timezone;

	public FitbitSleepResult(JsonNode node, Identity author, DateTimeZone timezone) {
		this.node = node;
		this.author = author;
		this.timezone = timezone;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode item : node.path("sleep")) {
			Event event = new Event();
			event.setValue(Event.TAG, "sleep");
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.TIMESTAMP, LocalDateTime.parse(item.path("startTime").getTextValue()).toDateTime(timezone));
			event.setValue(Event.DURATION, Duration.millis(item.path("duration").getLongValue()));
			event.setValue(Event.RATING, Rating.valueOf(item.path("efficiency").getIntValue()));
			events.add(event);
		}
		return events;
	}
}
