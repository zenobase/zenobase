package com.zenobase.tasks.nokia;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import play.Logger;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Resource;

class NokiaHealthSleepResult {

	public static final Resource SOURCE = new Resource("Nokia Health", "https://health.nokia.com/");

	private final ObjectNode node;
	private final Identity author;
	private final String tag;
	private final boolean useRanges;
	private final DateTimeZone timezone;

	public NokiaHealthSleepResult(ObjectNode node, Identity author, String tag, boolean useRanges, DateTimeZone timezone) {
		this.node = node;
		this.author = author;
		this.tag = tag;
		this.useRanges = useRanges;
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
			Logger.warn("Missing a start or end date: {}", node);
			return null;
		}
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, begin);
		if (useRanges) {
			event.addValue(Event.TIMESTAMP, end);
		}
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.setValue(Event.PERCENTAGE, percentageValue(node.path("state")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private static DateTime dateTimeValue(JsonNode node, DateTimeZone timezone) {
		return !node.isMissingNode() ? new DateTime(node.longValue() * 1000L, timezone) : null;
	}

	private static Percentage percentageValue(JsonNode node) {
		return Percentage.valueOf(node.intValue() > 0 ? 100 : 0);
	}

	public static List<Event> merge(List<Event> events) {
		List<Event> merged = Lists.newArrayList();
		Event prev = null;
		DateTime end = null;
		for (Event event : events) {
			DateTime begin = getBegin(event);
			Duration duration = event.getValue(Event.DURATION);
			if (prev != null && end != null && end.equals(begin)) {
				prev.setValue(Event.PERCENTAGE, meanPercentage(prev, event));
				prev.setValue(Event.DURATION, prev.getValue(Event.DURATION).plus(duration));
				end = getEnd(event);
				prev.setValues(Event.TIMESTAMP, ImmutableList.of(getBegin(prev), end));
				continue;
			}
			merged.add(event);
			prev = event;
			end = getEnd(event);
		}
		return merged;
	}

	private static DateTime getBegin(Event event) {
		return Ordering.natural().min(event.getValues(Event.TIMESTAMP));
	}

	private static DateTime getEnd(Event event) {
		return Ordering.natural().max(event.getValues(Event.TIMESTAMP));
	}

	private static Percentage meanPercentage(Event left, Event right) {
		return mean(left.getValue(Event.PERCENTAGE), Ints.checkedCast(left.getValue(Event.DURATION).getStandardSeconds()),
			right.getValue(Event.PERCENTAGE), Ints.checkedCast(right.getValue(Event.DURATION).getStandardSeconds()));
	}

	private static Percentage mean(Percentage left, int leftWeight, Percentage right, int rightWeight) {
		return Percentage.valueOf((left.getValue().intValue() * leftWeight + right.getValue().intValue() * rightWeight) / (leftWeight + rightWeight));
	}
}
