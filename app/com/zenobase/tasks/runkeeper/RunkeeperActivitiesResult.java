package com.zenobase.tasks.runkeeper;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;

import com.zenobase.common.Measures;
import com.zenobase.common.Pace;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class RunkeeperActivitiesResult extends RunkeeperResultSupport {

	private final Unit<Length> lengthUnit;
	private final Unit<Energy> energyUnit;

	public RunkeeperActivitiesResult(JsonNode node, Identity author, Unit<Length> lengthUnit, Unit<Energy> energyUnit, DateTimeZone timezone) {
		super(node, author, timezone);
		this.lengthUnit = lengthUnit;
		this.energyUnit = energyUnit;
	}

	@Override
	protected Event newEvent(JsonNode node) {
		Event event = new Event();
		Duration duration = durationValue(node.path("duration"));
		DecimalMeasure<Length> distance = distanceValue(node.path("total_distance"));
		event.addValue(Event.TAG, node.path("type").textValue());
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("start_time"), dateTimeZoneValue(node.path("utc_offset"))));
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

	private DateTimeZone dateTimeZoneValue(JsonNode node) {
		if (node.isNumber()) {
			int offset = BigDecimal.valueOf(node.doubleValue() * 60.0).intValueExact();
			return DateTimeZone.forOffsetHoursMinutes(offset / 60, offset % 60);
		}
		return timezone;
	}

	private Duration durationValue(JsonNode node) {
		return !isZero(node) ? Duration.millis(node.decimalValue().movePointRight(3).longValue()) : null;
	}

	private DecimalMeasure<Length> distanceValue(JsonNode node) {
		return !isZero(node) ? Measures.valueOf(Measures.convert(node.doubleValue(), lengthUnit), lengthUnit) : null;
	}

	private DecimalMeasure<Energy> energyValue(JsonNode node) {
		return !isZero(node) ? Measures.<Energy>valueOf(Measures.round(node.decimalValue(), 0), energyUnit) : null;
	}

	private DecimalMeasure<Velocity> calculateVelocity(DecimalMeasure<Length> distance, Duration duration) {
		Unit<Velocity> unit = Units.isMetric(lengthUnit) ? Units.KMH : Units.MPH;
		long t = duration.getStandardSeconds();
		double d = Measures.toStandard(distance).getValue().doubleValue();
		return t * d > 0.0 ? Measures.valueOf(Measures.round(Measures.convert(d / t, unit), 1), unit) : null;
	}

	private DecimalMeasure<Pace> calculatePace(Duration duration, DecimalMeasure<Length> distance) {
		Unit<Pace> unit = Units.isMetric(lengthUnit) ? Units.S_PER_KM : Units.S_PER_MI;
		long t = duration.getStandardSeconds();
		double d = Measures.toStandard(distance).getValue().doubleValue();
		return d * t > 0.0 ? Measures.valueOf(Measures.round(Measures.convert(t / d, unit), 0), unit) : null;
	}
}
