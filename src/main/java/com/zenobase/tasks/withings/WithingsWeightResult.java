package com.zenobase.tasks.withings;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;

class WithingsWeightResult extends WithingsResult {

	private final Unit<Mass> unit;
	private final DateTimeZone timezone;

	public WithingsWeightResult(ObjectNode node, Identity author, String tag, Unit<Mass> unit, DateTimeZone timezone) {
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
		var event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, getDateTime(node, timezone));
		int count = 0;
		for (JsonNode measure : node.path("measures")) {
			switch (measure.path("type").intValue()) {
				case 1 -> { // weight
					event.setValue(Event.WEIGHT, getDecimalMeasure(measure, unit));
					++count;
				}
				case 6 -> { // fat %
					event.setValue(
							Event.PERCENTAGE, Percentage.valueOf(Objects.requireNonNull(getBigDecimal(measure))));
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

	private static @Nullable DecimalMeasure<Mass> getDecimalMeasure(JsonNode measure, Unit<Mass> unit) {
		BigDecimal value = getBigDecimal(measure);
		return value != null ? new DecimalMeasure<>(value, Units.KG).to(unit, new MathContext(5)) : null;
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
