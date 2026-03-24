package com.zenobase.tasks.google;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

public class SessionsResult extends GoogleFitResultSupport {

	private final Identity author;

	public SessionsResult(JsonNode node, Identity author, DateTimeZone zone) {
		super(node, zone);
		this.author = author;
	}

	public String getNextPageToken() {
		return node.path("nextPageToken").textValue();
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode sessionNode : node.path("session")) {
			addEvent(sessionNode, events);
		}
		return events;
	}

	private void addEvent(JsonNode node, List<Event> events) {
		Event event = new Event();
		DateTime begin = dateTimeValue(node.path("startTimeMillis"));
		DateTime end = dateTimeValue(node.path("endTimeMillis"));
		event.addValue(Event.TIMESTAMP, begin);
		event.addValue(Event.TIMESTAMP, end);
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.setValue(Event.TAG, activityTypeValue(node.path("activityType")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, resourceValue(node.path("application")));
		events.add(event);
	}
}
