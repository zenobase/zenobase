package com.zenobase.tasks.withings;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class WithingsStepsResult {

	public static final Resource SOURCE = new Resource("Withings", "http://withings.com/");

	private final ObjectNode node;
	private final Identity author;
	private final String tag;
	private final Unit<Length> unit;
	private final DateTimeZone timezone;

	public WithingsStepsResult(ObjectNode node, Identity author, String tag, Unit<Length> unit, DateTimeZone timezone) {
		// System.err.println("result:" + node);
		this.node = node;
		this.author = author;
		this.tag = tag;
		this.unit = unit;
		this.timezone = timezone;
	}

	public int getStatus() {
		return node.path("status").isInt() ? node.path("status").intValue() : -1;
	}

	public String getMarker() {
		return Strings.emptyToNull(node.path("body").path("updatetime").asText());
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		int size = node.path("body").path("series").size();
		if (size > 0) {
			ObjectNode series = (ObjectNode) node.path("body").path("series");
			for (Iterator<Map.Entry<String, JsonNode>> i = series.fields(); i.hasNext();) {
				Map.Entry<String, JsonNode> entry = i.next();
				addEvents(Long.parseLong(entry.getKey()) * 1000L, (ObjectNode) entry.getValue(), events);
			}
		}
		System.err.println(begin + " -> " + end);
		return events;
	}

	private DateTime begin, end;

	private void addEvents(long time, ObjectNode node, List<Event> events) {
		DateTime begin = new DateTime(time, timezone);
		Duration duration = getDuration(node.path("duration"));
		DateTime end = begin.plus(duration);
		if (this.begin == null || this.begin.isAfter(begin)) {
			this.begin = begin;
		}
		if (this.end == null || this.end.isBefore(end)) {
			this.end = end;
		}
		//System.out.println(begin + " -> " + end);
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, begin);
		// steps
		// elevation
		// distance
		// calories
		event.setValue(Event.DISTANCE, getDecimalMeasure(node.path("distance"), unit));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		events.add(event);
	}

	private static Duration getDuration(JsonNode node) {
		return Duration.standardSeconds(node.intValue());
	}

	private static <Q extends Quantity> DecimalMeasure<Q> getDecimalMeasure(JsonNode node, Unit<Q> unit) {
		return DecimalMeasure.valueOf(node.decimalValue(), unit);
	}
}
