package com.zenobase.tasks.withings;

import java.util.List;

import org.elasticsearch.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import play.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class WithingsSleepResult {

	public static final Resource SOURCE = new Resource("Withings", "http://withings.com/");

	private final ObjectNode node;
	private final Identity author;
	private final String tag;
	private final DateTimeZone timezone;

	public WithingsSleepResult(ObjectNode node, Identity author, String tag, DateTimeZone timezone) {
		this.node = node;
		this.author = author;
		this.tag = tag;
		this.timezone = timezone;
	}

	public int getStatus() {
		return node.path("status").isInt() ? node.path("status").intValue() : -1;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode seriesNode : node.path("body").path("series")) {
			Event event = getEvent(seriesNode);
			if (event != null) {
				events.add(event);
			}
		}
		return events;
	}

	private Event getEvent(JsonNode node) {
		DateTime begin = dateTimeValue(node.path("startdate"), timezone);
		DateTime end = dateTimeValue(node.path("enddate"), timezone);
		if (begin == null || end == null) {
			Logger.warn("Missing a start or end date: " + node);
			return null;
		}
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, begin);
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private static DateTime dateTimeValue(JsonNode node, DateTimeZone timezone) {
		return !node.isMissingNode() ? new DateTime(node.longValue() * 1000L, timezone) : null;
	}

	public static List<Event> merge(List<Event> events) {
		List<Event> merged = Lists.newArrayList();
		Event prev = null;
		DateTime end = null;
		for (Event event : events) {
			DateTime begin = event.getValue(Event.TIMESTAMP);
			Duration duration = event.getValue(Event.DURATION);
			if (prev != null && end != null && end.equals(begin)) {
				prev.setValue(Event.DURATION, prev.getValue(Event.DURATION).plus(duration));
				end = end.plus(duration);
				continue;
			}
			merged.add(event);
			prev = event;
			end = event.getValue(Event.TIMESTAMP).plus(event.getValue(Event.DURATION));
		}
		return merged;
	}
}
