package com.zenobase.tasks.sleepcloud;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;

public class SleepCloudResult {

	public static final Resource SOURCE = new Resource("SleepCloud", "https://sleep-cloud.appspot.com/");

	private final String tag;
	private final Identity author;
	private final boolean useRanges;
	private final JsonNode node;

	public SleepCloudResult(String tag, Identity author, boolean useRanges, JsonNode node) {
		this.tag = tag;
		this.author = author;
		this.useRanges = useRanges;
		this.node = node;
	}

	public String getCursor() {
		return node.path("cursor").textValue();
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode sleepNode : node.path("sleeps")) {
			addSleep(sleepNode, events);
		}
		return events;
	}

	private void addSleep(JsonNode node, List<Event> events) {
		var event = new Event();
		event.addValue(Event.TAG, tag);
		for (JsonNode tagNode : node.path("tags")) {
			event.addValue(Event.TAG, tagNode.textValue());
		}
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		DateTimeZone zone = dateTimeZoneValue(node.path("timezone"));
		DateTime begin = dateTimeValue(node.path("fromTime"), zone);
		DateTime end = dateTimeValue(node.path("toTime"), zone);
		event.setValue(Event.TIMESTAMP, begin);
		if (useRanges) {
			event.addValue(Event.TIMESTAMP, end);
		}
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.setValue(Event.COUNT, countValue(node.path("cycles")));
		event.setValue(Event.RATING, ratingValue(node.path("rating")));
		event.setValue(Event.PERCENTAGE, percentageValue(node.path("deepSleep")));
		event.setValue(Event.NOTE, node.path("comment").textValue());
		events.add(event);
	}

	private DateTimeZone dateTimeZoneValue(JsonNode node) {
		if (node.isMissingNode()) {
			return DateTimeZone.UTC;
		}
		String value =
				node.textValue().replace("GMT--", "-").replace("GMT-", "-").replace("GMT+", "+");
		if (value.length() < 6) {
			value = value.charAt(0) + "0" + value.substring(1);
		}
		Preconditions.checkArgument(value.length() == 6, "Invalid timezone: %s", node);
		return DateTimeZone.forID(value);
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
