package com.zenobase.tasks.runkeeper;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.common.Pace;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class ActivitiesResult {

	static final DateTimeFormatter TIME_FORMAT = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss");

	private final JsonNode node;
	private final Identity author;
	private final Unit<Length> lengthUnit;
	private final Unit<Energy> energyUnit;
	private final DateTimeZone timezone;

	public ActivitiesResult(JsonNode node, Identity author, Unit<Length> lengthUnit, Unit<Energy> energyUnit, DateTimeZone timezone) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.lengthUnit = lengthUnit;
		this.energyUnit = energyUnit;
		this.timezone = timezone;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode itemNode : node.path("items")) {
			events.add(newEvent(itemNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		Event event = new Event();
		Duration duration = durationValue(node.path("duration"));
		DecimalMeasure<Length> distance = distanceValue(node.path("total_distance"));
		event.addValue(Event.TAG, node.path("type").textValue());
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("start_time")));
		event.setValue(Event.DURATION, duration);
		event.setValue(Event.DISTANCE, distance);
		if (distance != null && duration != null) {
			event.setValue(Event.VELOCITY, calculateVelocity(distance, duration));
			event.setValue(Event.PACE, calculatePace(duration, distance));
		}
		event.setValue(Event.ENERGY, energyValue(node.path("total_calories")));
		event.setValue(Event.SOURCE, new Resource("RunKeeper", node.path("uri").textValue()));
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	private DateTime dateTimeValue(JsonNode node) {
		LocalDateTime local = TIME_FORMAT.parseLocalDateTime(node.textValue());
		Preconditions.checkState(!timezone.isLocalDateTimeGap(local), "<%s> does not exist in <%s>", local, timezone);
		return local.toDateTime(timezone);
	}

	private Duration durationValue(JsonNode node) {
		return node.isNumber() ? Duration.millis(node.decimalValue().movePointRight(3).longValue()) : null;
	}

	private DecimalMeasure<Length> distanceValue(JsonNode node) {
		return node.isNumber() ? Measures.valueOf(Measures.convert(node.doubleValue(), lengthUnit), lengthUnit) : null;
	}

	private DecimalMeasure<Energy> energyValue(JsonNode node) {
		return node.isNumber() ? Measures.<Energy>valueOf(Measures.round(node.decimalValue(), 0), energyUnit) : null;
	}

	private DecimalMeasure<Velocity> calculateVelocity(DecimalMeasure<Length> distance, Duration duration) {
		Unit<Velocity> unit = Units.isMetric(lengthUnit) ? Units.KMH : Units.MPH;
		long t = duration.getStandardSeconds();
		double d = Measures.toStandard(distance).getValue().doubleValue();
		return t > 0 ? Measures.valueOf(Measures.convert(d / t, unit), unit) : null;
	}

	private DecimalMeasure<Pace> calculatePace(Duration duration, DecimalMeasure<Length> distance) {
		Unit<Pace> unit = Units.isMetric(lengthUnit) ? Units.S_PER_KM : Units.S_PER_MI;
		long t = duration.getStandardSeconds();
		double d = Measures.toStandard(distance).getValue().doubleValue();
		return d > 0.0 ? Measures.valueOf(Measures.round(Measures.convert(t / d, unit), 0), unit) : null;
	}

	public String getNext() {
		return node.path("next").textValue();
	}
}
