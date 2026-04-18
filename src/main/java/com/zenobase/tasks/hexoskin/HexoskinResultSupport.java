package com.zenobase.tasks.hexoskin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Resource;

abstract class HexoskinResultSupport {

	static final Resource SOURCE = new Resource("Hexoskin", "https://www.hexoskin.com/");

	private final JsonNode node;
	private final Identity author;
	private final @Nullable String tag;
	private final DateTimeZone zone;
	private final Unit<Length> distanceUnit;

	public HexoskinResultSupport(
		JsonNode node,
		Identity author,
		@Nullable String tag,
		DateTimeZone zone,
		boolean metric
	) {
		this.node = node;
		this.author = author;
		this.tag = tag;
		this.zone = zone;
		this.distanceUnit = metric ? Units.KM : Units.MI;
	}

	public String next() {
		return node.path("meta").path("next").textValue();
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode objectNode : node.path("objects")) {
			Event event = newEvent(objectNode);
			if (event != null) {
				events.add(event);
			}
		}
		return events;
	}

	private @Nullable Event newEvent(JsonNode node) {
		if (ignore(node)) {
			return null;
		}
		var event = new Event();
		DateTime t0 = dateTimeValue(node.path("start_date"));
		DateTime t1 = dateTimeValue(node.path("end"));
		event.addValue(Event.TIMESTAMP, t0);
		event.addValue(Event.TIMESTAMP, t1);
		event.setValue(Event.DURATION, new Duration(t0, t1));
		event.setValue(Event.NOTE, textValue(node.path("note")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.RESOURCE, resourceValue(node));
		event.setValue(Event.SOURCE, SOURCE);
		for (JsonNode metricNode : node.path("metrics")) {
			setMetric(event, metricNode);
		}
		if (tag != null) {
			event.addValue(Event.TAG, tag);
		}
		return event;
	}

	protected boolean ignore(JsonNode node) {
		return !"complete".equals(node.path("status").textValue());
	}

	private void setMetric(Event event, JsonNode node) {
		switch (node.path("name").textValue()) {
			case "heartrate_avg" -> event.setValue(Event.FREQUENCY, frequencyValue(node.path("value")));
			case "step_count" -> event.setValue(Event.COUNT, intValue(node.path("value")));
			case "SleepEfficiency" -> event.setValue(Event.PERCENTAGE, percentageValue(node.path("value")));
			case "energyecg_total_kcal" -> event.setValue(Event.ENERGY, energyValue(node.path("value")));
			case "distance" -> event.setValue(Event.DISTANCE, distanceValue(node.path("value")));
		}
	}

	private DateTime dateTimeValue(JsonNode node) {
		if (node.isTextual()) {
			return DateTime.parse(node.textValue()).withZone(zone);
		} else if (node.isLong()) {
			return new DateTime((1000 * node.longValue()) / 256, zone);
		} else {
			throw new IllegalArgumentException("Can't parse time: " + node);
		}
	}

	private Resource resourceValue(JsonNode node) {
		return new Resource(
			node.path("name").textValue(),
			"https://my.hexoskin.com/en/activities/" + node.path("id").longValue()
		);
	}

	private @Nullable DecimalMeasure<Frequency> frequencyValue(JsonNode node) {
		return !isZero(node)
			? Measures.valueOf(Objects.requireNonNull(Measures.round(node.decimalValue(), 0)), Units.BPM)
			: null;
	}

	private @Nullable Integer intValue(JsonNode node) {
		return !isZero(node) ? node.intValue() : null;
	}

	private @Nullable Percentage percentageValue(JsonNode node) {
		return !isZero(node) ? Percentage.valueOf(node.intValue()) : null;
	}

	private @Nullable DecimalMeasure<Length> distanceValue(JsonNode node) {
		return !isZero(node) && isPositive(node)
			? Measures.valueOf(
					Objects.requireNonNull(Measures.round(Measures.convert(node.doubleValue(), distanceUnit), 2)),
					distanceUnit
				)
			: null;
	}

	private @Nullable DecimalMeasure<Energy> energyValue(JsonNode node) {
		return !isZero(node)
			? Measures.valueOf(Objects.requireNonNull(Measures.round(node.decimalValue(), 0)), Units.KCAL)
			: null;
	}

	private @Nullable String textValue(JsonNode node) {
		return Strings.emptyToNull(node.textValue());
	}

	private static boolean isZero(JsonNode node) {
		Preconditions.checkArgument(node.isMissingNode() || node.isNull() || node.isNumber());
		return node.doubleValue() == 0.0;
	}

	private static boolean isPositive(JsonNode node) {
		return node.doubleValue() > 0.0;
	}
}
