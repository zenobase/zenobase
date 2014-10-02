package com.zenobase.tasks.jawbone;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class SleepResult extends JawboneResult {

	public SleepResult(JsonNode node, Identity author, String tag) {
		super(node, author, tag);
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode sleepNode : node.path("items")) {
			events.add(newEvent(sleepNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("time_created"), dateTimeZoneValue(node.path("details").path("tz"))));
		event.setValue(Event.DURATION, durationValue(node.path("details").path("duration")));
		event.setValue(Event.RATING, ratingValue(node.path("details").path("quality")));
		event.setValue(Event.LOCATION, locationValue(node));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}
}
