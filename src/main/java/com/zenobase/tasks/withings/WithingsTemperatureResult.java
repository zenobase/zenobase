package com.zenobase.tasks.withings;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Temperature;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class WithingsTemperatureResult extends WithingsResult {

	private final @Nullable Unit<Temperature> unit;
	private final DateTimeZone timezone;

	public WithingsTemperatureResult(
			ObjectNode node,
			Identity author,
			@Nullable String tag,
			@Nullable Unit<Temperature> unit,
			DateTimeZone timezone) {
		super(node, author, tag);
		this.unit = unit;
		this.timezone = timezone;
	}

	@Override
	public @Nullable String getMarker() {
		return Strings.emptyToNull(node.path("body").path("updatetime").asText());
	}

	@Override
	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode group : node.path("body").path("measuregrps")) {
			addEvents(group, events);
		}
		return events;
	}

	private void addEvents(JsonNode node, List<Event> events) {
		for (JsonNode measure : node.path("measures")) {
			switch (measure.path("type").intValue()) {
				case 12, 71, 73 -> { // air/body/skin temperature
					var event = new Event();
					event.setValue(Event.TAG, tag);
					event.setValue(Event.TIMESTAMP, getDateTime(node, timezone));
					event.setValue(Event.TEMPERATURE, getDecimalMeasure(measure, unit));
					event.setValue(Event.AUTHOR, author);
					event.setValue(Event.SOURCE, SOURCE);
					events.add(event);
				}
			}
		}
	}

	private static @Nullable DecimalMeasure<Temperature> getDecimalMeasure(
			JsonNode measure, @Nullable Unit<Temperature> unit) {
		BigDecimal value = getBigDecimal(measure);
		if (value == null || unit == null) {
			return null;
		}
		return Measures.round(Measures.valueOf(value, Units.C).to(unit, new MathContext(5)), 3);
	}

	private static @Nullable BigDecimal getBigDecimal(JsonNode node) {
		int value = node.path("value").intValue();
		int scale = node.path("unit").intValue();
		return value != 0 ? BigDecimal.valueOf(value, -scale) : null;
	}

	private static DateTime getDateTime(JsonNode node, DateTimeZone timezone) {
		return new DateTime(node.path("date").longValue() * 1000, timezone);
	}
}
