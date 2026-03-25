package com.zenobase.tasks.fitbit;

import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class FitbitBurnResult extends FitbitResultSupport {

	public FitbitBurnResult(JsonNode node, String tag, Identity author, DateTimeZone timezone) {
		super(node, tag, author, timezone);
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode dayNode : node.path("activities-calories")) {
			events.add(newEvent(dayNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		DateTime begin = dateTimeValue(node.path("dateTime"));
		var event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, begin);
		event.setValue(Event.DURATION, Period.days(1).toDurationFrom(begin));
		event.setValue(Event.ENERGY, energyValue(node.path("value"), Units.KCAL));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
