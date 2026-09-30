package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.measure.quantity.Temperature;
import javax.measure.unit.Unit;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

/**
 * Parses responses from {@code dailySleepTemperatureDerivation:list}. Each entry has a {@code startTime} and a delta
 * (or absolute) temperature in Celsius.
 */
class GoogleHealthTemperatureResult extends GoogleHealthResultSupport {

	private final boolean metric;

	GoogleHealthTemperatureResult(
		JsonNode node,
		@Nullable String tag,
		Identity author,
		DateTimeZone timezone,
		boolean metric
	) {
		super(node, tag, author, timezone);
		this.metric = metric;
	}

	List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode item : node.path("dailySleepTemperatureDerivationValues")) {
			JsonNode value = item.path("celsius");
			if (!value.isNumber()) {
				continue;
			}
			BigDecimal celsius = value.decimalValue();
			Event event = new Event();
			event.setValue(Event.TAG, tag != null ? tag : "temperature");
			event.setValue(Event.TIMESTAMP, dateTimeValue(item.path("startTime"), timezone));
			Unit<Temperature> unit = metric ? Units.C : Units.F;
			BigDecimal converted = Objects.requireNonNull(Measures.convert(celsius.doubleValue(), unit));
			event.setValue(Event.TEMPERATURE, Measures.valueOf(converted, unit));
			event.setValue(Event.AUTHOR, author);
			setSources(event, item.path("origin"));
			events.add(event);
		}
		return events;
	}
}
