package com.zenobase.tasks.fitbit;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.Period;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class FitbitStepsResult extends FitbitResultSupport {

	private final LocalDate date;
	private final Unit<Length> distanceUnit, heightUnit;
	private final Unit<Energy> energyUnit;
	private final boolean includeBMR;

	public FitbitStepsResult(JsonNode node, String tag, Identity author, LocalDate date, DateTimeZone timezone, Unit<Length> distanceUnit, Unit<Length> heightUnit, Unit<Energy> energyUnit, boolean includeBMR) {
		super(node, tag, author, timezone);
		this.date = date;
		this.distanceUnit = distanceUnit;
		this.heightUnit = heightUnit;
		this.energyUnit = energyUnit;
		this.includeBMR = includeBMR;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		int steps = node.path("summary").path("steps").intValue();
		if (steps > 0) {
			DateTime begin = date.toDateTimeAtStartOfDay(timezone);
			Event event = new Event();
			event.setValue(Event.TAG, tag);
			event.setValue(Event.TIMESTAMP, begin);
			event.setValue(Event.DURATION, Period.days(1).toDurationFrom(begin));
			event.setValue(Event.COUNT, steps);
			event.setValue(Event.DISTANCE, getDistance());
			event.setValue(Event.HEIGHT, lengthValue(node.path("summary").path("elevation"), heightUnit));
			event.setValue(Event.ENERGY, energyValue(node.path("summary").path(includeBMR ? "caloriesOut" : "activityCalories"), energyUnit));
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
			events.add(event);
		}
		return events;
	}

	private DecimalMeasure<Length> getDistance() {
		for (JsonNode distance : node.path("summary").path("distances")) {
			if ("total".equals(distance.path("activity").textValue())) {
				return lengthValue(distance.path("distance"), distanceUnit);
			}
		}
		return null;
	}
}
