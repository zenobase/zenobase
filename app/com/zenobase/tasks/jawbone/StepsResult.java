package com.zenobase.tasks.jawbone;

import java.util.List;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class StepsResult extends JawboneResult {

	private final boolean hourly;
	private final boolean metric;

	public StepsResult(JsonNode node, Identity author, String tag, boolean hourly, boolean metric) {
		super(node, author, tag);
		System.err.println(node);
		this.hourly = hourly;
		this.metric = metric;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode dayNode : node.path("items")) {
			events.add(newEvent(dayNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		DateTimeZone zone = dateTimeZoneValue(node.path("details").path("tz"));
		DateTime begin = dateTimeValue(node.path("time_created"), zone);
		DateTime end = dateTimeValue(node.path("time_completed"), zone);
		event.setValue(Event.TIMESTAMP, begin);
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.setValue(Event.COUNT, node.path("details").path("steps").intValue());
		event.setValue(Event.DISTANCE, distanceValue(node.path("details").path("distance"), metric ? SI.KILOMETER : NonSI.MILE));
		event.setValue(Event.ENERGY, energyValue(node.path("details").path("calories")));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}
}
