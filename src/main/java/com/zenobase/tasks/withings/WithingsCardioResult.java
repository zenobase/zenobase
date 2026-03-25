package com.zenobase.tasks.withings;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;

class WithingsCardioResult extends WithingsResult {

	private final DateTimeZone timezone;

	public WithingsCardioResult(ObjectNode node, Identity author, String tag, DateTimeZone timezone) {
		super(node, author, tag);
		this.timezone = timezone;
	}

	public String getMarker() {
		return Strings.emptyToNull(node.path("body").path("updatetime").asText());
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode group : node.path("body").path("measuregrps")) {
			addEvents(group, events);
		}
		return events;
	}

	private void addEvents(JsonNode node, List<Event> events) {
		var event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, getDateTime(node, timezone));
		int count = 0;
		for (JsonNode measure : node.path("measures")) {
			switch (measure.path("type").intValue()) {
				case 9, 10 -> { // diastolic/systolic blood pressure
					event.addValue(Event.PRESSURE, getDecimalMeasure(measure, Units.MMHG));
					++count;
				}
				case 11 -> { // heart rate
					event.setValue(Event.FREQUENCY, getDecimalMeasure(measure, Units.BPM));
					++count;
				}
				case 54 -> { // SpO2
					event.setValue(Event.PERCENTAGE, getPercentage(measure));
					++count;
				}
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
