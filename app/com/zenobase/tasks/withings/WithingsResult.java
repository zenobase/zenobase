package com.zenobase.tasks.withings;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Mass;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class WithingsResult {

	public static final Resource SOURCE = new Resource("Withings", "http://withings.com/");

	private final ObjectNode node;
	private final Identity author;
	private final String tag;
	private final Unit<Mass> unit;
	private final DateTimeZone timezone = DateTimeZone.UTC;

	public WithingsResult(ObjectNode node, Identity author, String tag, Unit<Mass> unit) {
		this.node = node;
		this.author = author;
		this.tag = tag;
		this.unit = unit;
	}

	public int getStatus() {
		return node.path("status").isInt() ? node.path("status").intValue() : -1;
	}

	public String getMarker() {
		return Strings.emptyToNull(node.path("body").path("updatetime").asText());
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode group : node.path("body").path("measuregrps")) {
			addEvents(group, events);
		}
		return events;
	}

	private void addEvents(JsonNode node, List<Event> events) {
		for (JsonNode measure : node.path("measures")) {
			switch (measure.path("type").intValue()) {
				case 1: // weight
					Event event = new Event();
					event.setValue(Event.TAG, tag);
					event.setValue(Event.WEIGHT, getDecimalMeasure(measure, unit));
					event.setValue(Event.TIMESTAMP, getDateTime(node, timezone));
					event.setValue(Event.AUTHOR, author);
					event.setValue(Event.SOURCE, SOURCE);
					events.add(event);
			}
		}
	}

	private static DecimalMeasure<Mass> getDecimalMeasure(JsonNode measure, Unit<Mass> unit) {
		BigDecimal value = getBigDecimal(measure);
		return value != null ? new DecimalMeasure<Mass>(value, SI.KILOGRAM).to(unit, new MathContext(5)) : null;
	}

	private static BigDecimal getBigDecimal(JsonNode node) {
		int value = node.path("value").intValue();
		int scale = node.path("unit").intValue();
		return value != 0 ? BigDecimal.valueOf(value, -scale) : null;
	}

	private static DateTime getDateTime(JsonNode node, DateTimeZone timezone) {
		return new DateTime(node.path("date").longValue() * 1000, timezone);
	}
}
