package com.zenobase.tasks.fitbit;

import java.util.List;

import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class FitbitActivitiesResult extends FitbitResultSupport {

	private final Unit<Length> distanceUnit;

	public FitbitActivitiesResult(JsonNode node, Identity author, DateTimeZone timezone, Unit<Length> distanceUnit) {
		super(node, null, author, timezone);
		this.distanceUnit = distanceUnit;
	}

	@Override
	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode item : node.path("activities")) {
			if (item.path("hasStartTime").booleanValue()) {
				LocalDate date = LocalDate.parse(item.path("startDate").textValue());
				LocalTime time = LocalTime.parse(item.path("startTime").textValue());
				Event event = new Event();
				event.setValue(Event.TAG, item.path("activityParentName").textValue());
				event.setValue(Event.TIMESTAMP, date.toDateTime(time, timezone));
				event.setValue(Event.DURATION, durationValue(item.path("duration")));
				event.setValue(Event.COUNT, item.path("steps").intValue());
				event.setValue(Event.DISTANCE, lengthValue(item.path("distance"), distanceUnit));
				event.setValue(Event.ENERGY, energyValue(item.path("calories"), Units.KCAL));
				event.setValue(Event.AUTHOR, author);
				event.setValue(Event.SOURCE, SOURCE);
				events.add(event);
			}
		}
		return events;
	}
}
