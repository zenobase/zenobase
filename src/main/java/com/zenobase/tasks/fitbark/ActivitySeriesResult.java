package com.zenobase.tasks.fitbark;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class ActivitySeriesResult {

	static final Resource SOURCE = new Resource("FitBark", "https://www.fitbark.com/");

	private final String tag;
	private final Identity author;
	private final DateTime previous;
	private final DateTimeZone zone;
	private final ObjectNode node;

	public ActivitySeriesResult(String tag, Identity author, DateTime previous, DateTimeZone zone, ObjectNode node) {
		this.tag = tag;
		this.author = author;
		this.previous = previous;
		this.zone = zone;
		this.node = node;
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode recordNode : node.path("activity_series").path("records")) {
			addRecord(recordNode, events);
		}
		return events;
	}

	private void addRecord(JsonNode node, List<Event> events) {
		String date = node.path("date").textValue().replace(' ', 'T');
		boolean hourly = date.length() > 10;
		DateTime t = hourly
				? LocalDateTime.parse(date).toDateTime(zone)
				: LocalDate.parse(date).toDateTimeAtStartOfDay(zone);
		if (t.isAfter(previous)) {
			Event event = new Event();
			event.addValue(Event.TAG, tag);
			event.setValue(Event.SOURCE, SOURCE);
			event.setValue(Event.AUTHOR, author);
			event.addValue(Event.TIMESTAMP, t);
			event.setValue(Event.DURATION, new Duration(t, hourly ? t.plusHours(1) : t.plusDays(1)));
			event.setValue(Event.COUNT, node.path("activity_value").intValue());
			events.add(event);
		}
	}
}
