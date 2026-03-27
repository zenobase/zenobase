package com.zenobase.tasks.withings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class WithingsStepsResult extends WithingsResult {

	private final Unit<Length> distanceUnit, heightUnit;
	private final Unit<Energy> energyUnit;

	public WithingsStepsResult(
			ObjectNode node,
			Identity author,
			String tag,
			Unit<Length> distanceUnit,
			Unit<Length> heightUnit,
			Unit<Energy> energyUnit) {
		super(node, author, tag);
		this.distanceUnit = distanceUnit;
		this.heightUnit = heightUnit;
		this.energyUnit = energyUnit;
	}

	@Override
	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode activityNode : node.path("body").path("activities")) {
			events.add(getEvent((ObjectNode) activityNode));
		}
		return removeLatest(events);
	}

	private List<Event> removeLatest(List<Event> events) {
		if (events.size() < 2) {
			return Collections.emptyList();
		}
		events.sort((left, right) -> Objects.requireNonNull(right.getValue(Event.TIMESTAMP))
				.compareTo(Objects.requireNonNull(left.getValue(Event.TIMESTAMP))));
		return events.subList(1, events.size());
	}

	private Event getEvent(ObjectNode node) {
		DateTimeZone timezone = DateTimeZone.forID(node.path("timezone").textValue());
		DateTime time = dateTimeValue(node.path("date"), timezone);
		var event = new Event();
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
	public @Nullable String getMarker() {
		List<Event> events = getEvents();
		return !events.isEmpty()
				? Objects.requireNonNull(Objects.requireNonNull(Iterables.getLast(events))
								.getValue(Event.TIMESTAMP))
						.toLocalDate()
						.plusDays(1)
						.toString()
				: null;
	}

	private static DateTime dateTimeValue(JsonNode node, DateTimeZone timezone) {
		LocalDate date = LocalDate.parse(node.textValue());
		return date.toDateTimeAtStartOfDay(timezone);
	}

	private static <Q extends Quantity> DecimalMeasure<Q> measureValue(JsonNode node, Unit<Q> unit) {
		return DecimalMeasure.valueOf(node.decimalValue(), unit);
	}

	private static @Nullable <Q extends Quantity> DecimalMeasure<Q> convertMeasureValue(JsonNode node, Unit<Q> unit) {
		return node.isNumber()
				? Measures.valueOf(Objects.requireNonNull(Measures.convert(node.doubleValue(), unit)), unit)
				: null;
	}
}
