package com.zenobase.tasks.sleepcloud;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;

public class SleepsResult {

	public static final Resource SOURCE = new Resource("SleepCloud", "http://sleep-cloud.appspot.com/‎");

	private final String tag;
	private final Identity author;
	private final JsonNode node;

	public SleepsResult(String tag, Identity author, JsonNode node) {
		this.tag = tag;
		this.author = author;
		this.node = node;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode sleepNode : node.path("sleeps")) {
			addSleep(sleepNode, events);
		}
		return events;
	}

	private void addSleep(JsonNode node, List<Event> events) {
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		DateTimeZone zone = dateTimeZoneValue(node.path("timezone"));
		DateTime begin = dateTimeValue(node.path("fromTime"), zone);
		DateTime end = dateTimeValue(node.path("toTime"), zone);
		event.setValue(Event.TIMESTAMP, begin);
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.setValue(Event.COUNT, countValue(node.path("cycles")));
		event.setValue(Event.RATING, ratingValue(node.path("rating")));
		event.setValue(Event.PERCENTAGE, percentageValue(node.path("deepSleep")));
		events.add(event);
	}

	private DateTimeZone dateTimeZoneValue(JsonNode node) {
		if (node.isMissingNode()) {
			return DateTimeZone.UTC;
		}
		String value = node.textValue();
		Preconditions.checkArgument(value.startsWith("GMT"), "Invalid timezone: %s", node);
		return DateTimeZone.forID(value.substring(3));
	}

	private DateTime dateTimeValue(JsonNode node, DateTimeZone zone) {
		long value = node.longValue();
		Preconditions.checkArgument(value != 0L, "Can't find timestamp: %s", node);
		return new DateTime(value, zone);
	}

	private Integer countValue(JsonNode node) {
		int value = node.intValue();
		return value > 0 ? value : null;
	}

	private Rating ratingValue(JsonNode node) {
		double value = node.doubleValue();
		Preconditions.checkArgument(value >= 0.0 && value <= 5.0, "Invalid rating: %s", node);
		return value > 0.0 ? Rating.valueOf((int) (value * 20)) : null;
	}

	private Percentage percentageValue(JsonNode node) {
		double value = node.doubleValue();
		Preconditions.checkArgument(value >= 0.0 && value <= 1.0, "Invalid percentage: %s", node);
		return value > 0.0 ? Percentage.valueOf((int) (value * 100)) : null;
	}
}
