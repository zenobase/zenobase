package com.zenobase.tasks;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Quantity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class WithingsResult {

	public static final Resource SOURCE = new Resource("Withings", "http://withings.com/");

	private final String tag = "body";
	private final Unit<Mass> unit = SI.KILOGRAM;
	private final DateTimeZone timezone = DateTimeZone.UTC;
	private final Identity author;
	private final ObjectNode node;

	public WithingsResult(Identity author, ObjectNode node) {
		this.author = author;
		this.node = node;
		Preconditions.checkState(node.get("status").getIntValue() == 0);
	}

	public Long getMarker() {
		return node.path(tag).path("updatetime").getLongValue();
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode group : node.path(tag).path("measuregrps")) {
			addEvents(group, events);
		}
		return events;
	}

	private void addEvents(JsonNode node, List<Event> events) {
		for (JsonNode measure : node.path("measures")) {
			switch (measure.path("type").getIntValue()) {
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

	private static <T extends Quantity> DecimalMeasure<T> getDecimalMeasure(JsonNode measure, Unit<T> unit) {
		BigDecimal value = getBigDecimal(measure);
		return value != null ? new DecimalMeasure<T>(value, unit) : null;
	}

	private static BigDecimal getBigDecimal(JsonNode node) {
		int value = node.path("value").getIntValue();
		int scale = node.path("unit").getIntValue();
		return value != 0 ? BigDecimal.valueOf(value, -scale) : null;
	}

	private static DateTime getDateTime(JsonNode node, DateTimeZone timezone) {
		return new DateTime(node.path("date").getLongValue() * 1000, timezone);
	}
}
