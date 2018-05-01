package com.zenobase.tasks.nokia;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Resource;

class NokiaHealthCardioResult {

	private static final Resource SOURCE = new Resource("Nokia Health", "https://health.nokia.com/");

	private final ObjectNode node;
	private final Identity author;
	private final String tag;
	private final DateTimeZone timezone;

	public NokiaHealthCardioResult(ObjectNode node, Identity author, String tag, DateTimeZone timezone) {
		this.node = node;
		this.author = author;
		this.tag = tag;
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
		for (JsonNode group : node.path("body").path("measuregrps")) {
			addEvents(group, events);
		}
		return events;
	}

	private void addEvents(JsonNode node, List<Event> events) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, getDateTime(node, timezone));
		int count = 0;
		for (JsonNode measure : node.path("measures")) {
			switch (measure.path("type").intValue()) {
				case 9: // diastolic blood pressure
				case 10: // systolic blood pressure
					event.addValue(Event.PRESSURE, getDecimalMeasure(measure, Units.MMHG));
					++count;
					break;
				case 11: // heart rate
					event.setValue(Event.FREQUENCY, getDecimalMeasure(measure, Units.BPM));
					++count;
					break;
				case 54: // SpO2
					event.setValue(Event.PERCENTAGE, getPercentage(measure));
					++count;
					break;
			}
		}
		if (count > 0) {
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
			events.add(event);
		}
	}

	private static <Q extends Quantity> DecimalMeasure<Q> getDecimalMeasure(JsonNode node, Unit<Q> unit) {
		BigDecimal value = getBigDecimal(node);
		return value != null ? DecimalMeasure.valueOf(value, unit) : null;
	}

	private static BigDecimal getBigDecimal(JsonNode node) {
		int value = node.path("value").intValue();
		int scale = node.path("unit").intValue();
		return value != 0 ? BigDecimal.valueOf(value, -scale) : null;
	}

	private static Percentage getPercentage(JsonNode node) {
		BigDecimal value = getBigDecimal(node);
		return value != null && value.signum() > -1 ? Percentage.valueOf(value) : null;
	}

	private static DateTime getDateTime(JsonNode node, DateTimeZone timezone) {
		return new DateTime(node.path("date").longValue() * 1000, timezone);
	}
}
