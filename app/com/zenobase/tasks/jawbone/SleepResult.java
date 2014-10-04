package com.zenobase.tasks.jawbone;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class SleepResult extends JawboneResult {

	private final boolean useRanges;

	public SleepResult(JsonNode node, Identity author, String tag, boolean useRanges) {
		super(node, author, tag);
		this.useRanges = useRanges;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode sleepNode : node.path("items")) {
			events.add(newEvent(sleepNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		DateTime begin = dateTimeValue(node.path("time_created"), dateTimeZoneValue(node.path("details").path("tz")));
		Duration duration = durationValue(node.path("details").path("duration"));
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, begin);
		if (useRanges) {
			event.addValue(Event.TIMESTAMP, begin.plus(duration));
		}
		event.setValue(Event.DURATION, duration);
		event.setValue(Event.RATING, ratingValue(node.path("details").path("quality")));
		event.setValue(Event.LOCATION, locationValue(node));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}
}
