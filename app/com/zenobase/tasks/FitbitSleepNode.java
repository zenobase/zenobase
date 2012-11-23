package com.zenobase.tasks;

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Lists;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;

import com.zenobase.models.Event;
import com.zenobase.models.Rating;

class FitbitSleepNode {

	private final ObjectNode node;
	private final DateTimeZone timezone;

	public FitbitSleepNode(ObjectNode node, DateTimeZone timezone) {
		this.node = node;
		this.timezone = timezone;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode item : node.path("sleep")) {
			Event event = new Event();
			event.addValue(Event.TAG, "sleep");
			event.addValue(Event.TIMESTAMP, LocalDateTime.parse(item.path("startTime").getTextValue()).toDateTime(timezone));
			event.addValue(Event.DURATION, Duration.millis(item.path("duration").getLongValue()));
			event.addValue(Event.RATING, Rating.valueOf(item.path("efficiency").getIntValue()));
			events.add(event);
		}
		return events;
	}
}
