package com.zenobase.tasks.withings;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Temperature;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class WithingsTemperatureResult extends WithingsResult {

	private final Unit<Temperature> unit;
	private final DateTimeZone timezone;

	public WithingsTemperatureResult(ObjectNode node, Identity author, String tag, Unit<Temperature> unit, DateTimeZone timezone) {
		super(node, author, tag);
		this.unit = unit;
		this.timezone = timezone;
	}

	public String getMarker() {
		return Strings.emptyToNull(node.path("body").path("updatetime").asText());
	}

	@Override
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
				case 12: // ignore air (?) temperature
					break;
				case 71: // read body temperature
					event.setValue(Event.TEMPERATURE, getDecimalMeasure(measure, unit));
					++count;
					break;
				case 73: // ignore skin temperature
					break;
			}
		}
		if (count > 0) {
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
			events.add(event);
		}
	}

	private static DecimalMeasure<Temperature> getDecimalMeasure(JsonNode measure, Unit<Temperature> unit) {
		BigDecimal value = getBigDecimal(measure);
		return value != null ? Measures.round(Measures.valueOf(value, Units.C).to(unit, new MathContext(5)), 3) : null;
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
