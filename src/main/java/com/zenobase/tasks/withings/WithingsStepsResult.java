package com.zenobase.tasks.withings;

import java.util.Collections;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.Period;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class WithingsStepsResult extends WithingsResult {

	private final Unit<Length> distanceUnit, heightUnit;
	private final Unit<Energy> energyUnit;

	public WithingsStepsResult(ObjectNode node, Identity author, String tag, Unit<Length> distanceUnit, Unit<Length> heightUnit, Unit<Energy> energyUnit) {
		super(node, author, tag);
		this.distanceUnit = distanceUnit;
		this.heightUnit = heightUnit;
		this.energyUnit = energyUnit;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode activityNode : node.path("body").path("activities")) {
			events.add(getEvent((ObjectNode) activityNode));
		}
		return removeLatest(events);
	}

	private List<Event> removeLatest(List<Event> events) {
		if (events.size() < 2) {
			return Collections.emptyList();
		}
		events.sort((left, right) -> right.getValue(Event.TIMESTAMP).compareTo(left.getValue(Event.TIMESTAMP)));
		return events.subList(1, events.size());
	}

	private Event getEvent(ObjectNode node) {
		DateTimeZone timezone = DateTimeZone.forID(node.path("timezone").textValue());
		DateTime time = dateTimeValue(node.path("date"), timezone);
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, time);
		event.setValue(Event.DURATION, Period.days(1).toDurationFrom(time));
		event.setValue(Event.COUNT, node.path("steps").intValue());
		event.setValue(Event.ENERGY, measureValue(node.path("calories"), energyUnit));
		event.setValue(Event.DISTANCE, convertMeasureValue(node.path("distance"), distanceUnit));
		event.setValue(Event.HEIGHT, convertMeasureValue(node.path("elevation"), heightUnit));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	@Override
	public String getMarker() {
		List<Event> events = getEvents();
		return !events.isEmpty() ? Iterables.getLast(events).getValue(Event.TIMESTAMP).toLocalDate().plusDays(1).toString() : null;
	}

	private static DateTime dateTimeValue(JsonNode node, DateTimeZone timezone) {
		LocalDate date = LocalDate.parse(node.textValue());
		return date.toDateTimeAtStartOfDay(timezone);
	}

	private static <Q extends Quantity> DecimalMeasure<Q> measureValue(JsonNode node, Unit<Q> unit) {
		return DecimalMeasure.valueOf(node.decimalValue(), unit);
	}

	private static <Q extends Quantity> DecimalMeasure<Q> convertMeasureValue(JsonNode node, Unit<Q> unit) {
		return node.isNumber() ? Measures.valueOf(Measures.convert(node.doubleValue(), unit), unit) : null;
	}
}
