package com.zenobase.tasks.fitbit;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class FitbitCardioResult extends FitbitResultSupport {

	public FitbitCardioResult(JsonNode node, String tag, Identity author, DateTimeZone timezone) {
		super(node, tag, author, timezone);
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode dayNode : node.path("activities-restingHeartRate")) {
			events.add(newEvent(dayNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		DateTime begin = dateTimeValue(node.path("dateTime"));
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, begin);
		event.setValue(Event.DURATION, Period.days(1).toDurationFrom(begin));
		event.setValue(Event.FREQUENCY, frequencyValue(node.path("value")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
