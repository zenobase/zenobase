package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

/**
 * Parses responses from {@code weight:list} and {@code bodyFat:list}. Each entry carries a timestamp plus a scalar
 * measurement (kilograms for weight, integer percentage for body fat).
 */
class GoogleHealthWeightResult extends GoogleHealthResultSupport {

	private final boolean metric;

	GoogleHealthWeightResult(
		JsonNode node,
		@Nullable String tag,
		Identity author,
		DateTimeZone timezone,
		boolean metric
	) {
		super(node, tag, author, timezone);
		this.metric = metric;
	}

	List<Event> getWeightEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode item : node.path("weightRollupValues")) {
			BigDecimal kg = item.path("kilograms").decimalValue();
			if (kg.signum() <= 0) {
				continue;
			}
			Event event = baseEvent(item);
			Unit<Mass> unit = metric ? Units.KG : Units.LB;
			BigDecimal value = Objects.requireNonNull(Measures.convert(kg.doubleValue(), unit));
			event.setValue(Event.WEIGHT, Measures.valueOf(value, unit));
			events.add(event);
		}
		return events;
	}

	List<Event> getBodyFatEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode item : node.path("bodyFatRollupValues")) {
			int percent = item.path("percent").asInt();
			if (percent <= 0) {
				continue;
			}
			Event event = baseEvent(item);
			event.setValue(Event.PERCENTAGE, Percentage.valueOf(percent));
			events.add(event);
		}
		return events;
	}

	private Event baseEvent(JsonNode item) {
		DateTime begin = dateTimeValue(item.path("startTime"), timezone);
		Event event = new Event();
		if (tag != null) {
			event.setValue(Event.TAG, tag);
		}
		event.setValue(Event.TIMESTAMP, begin);
		event.setValue(Event.AUTHOR, author);
		setSources(event, item.path("origin"));
		return event;
	}
}
