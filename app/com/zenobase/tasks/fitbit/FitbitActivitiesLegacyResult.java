package com.zenobase.tasks.fitbit;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import com.zenobase.common.Measures;
import com.zenobase.common.Pace;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class FitbitActivitiesLegacyResult extends FitbitResultSupport {

	private final Unit<Length> distanceUnit;

	public FitbitActivitiesLegacyResult(JsonNode node, Identity author, DateTimeZone timezone, Unit<Length> distanceUnit) {
		super(node, null, author, timezone);
		this.distanceUnit = distanceUnit;
	}

	public String next() {
		return Strings.emptyToNull(node.path("pagination").path("next").textValue());
	}

	@Override
	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode item : node.path("activities")) {
			if (item.path("hasStartTime").booleanValue()) {
				LocalDate date = LocalDate.parse(item.path("startDate").textValue());
				LocalTime time = LocalTime.parse(item.path("startTime").textValue());
				Duration duration = durationValue(item.path("duration"));
				DecimalMeasure<Length> distance = lengthValue(item.path("distance"), distanceUnit);
				Event event = new Event();
				event.setValue(Event.TAG, item.path("activityParentName").textValue());
				event.setValue(Event.TIMESTAMP, date.toDateTime(time, timezone));
				event.setValue(Event.DURATION, duration);
				event.setValue(Event.COUNT, countValue(item.path("steps")));
				event.setValue(Event.DISTANCE, distance);
				if (duration != null && distance != null) {
					event.setValue(Event.VELOCITY, calculateVelocity(distance, duration));
					event.setValue(Event.PACE, calculatePace(duration, distance));
				}
				event.setValue(Event.ENERGY, energyValue(item.path("calories"), Units.KCAL));
				event.setValue(Event.AUTHOR, author);
				event.setValue(Event.SOURCE, SOURCE);
				events.add(event);
			}
		}
		return events;
	}

	private DecimalMeasure<Velocity> calculateVelocity(DecimalMeasure<Length> distance, Duration duration) {
		Unit<Velocity> unit = Units.isMetric(distanceUnit) ? Units.KMH : Units.MPH;
		long t = duration.getStandardSeconds();
		double d = Measures.toStandard(distance).getValue().doubleValue();
		return d * t > 0.0 ? Measures.valueOf(Measures.round(Measures.convert(d / t, unit), 1), unit) : null;
	}

	private DecimalMeasure<Pace> calculatePace(Duration duration, DecimalMeasure<Length> distance) {
		Unit<Pace> unit = Units.isMetric(distanceUnit) ? Units.S_PER_KM : Units.S_PER_MI;
		long t = duration.getStandardSeconds();
		double d = Measures.toStandard(distance).getValue().doubleValue();
		return t * d > 0.0 ? Measures.valueOf(Measures.round(Measures.convert(t / d, unit), 0), unit) : null;
	}

	public static boolean isLegacyResult(JsonNode node) {
		return !node.path("activities").path(0).path("startDate").isMissingNode();
	}
}
