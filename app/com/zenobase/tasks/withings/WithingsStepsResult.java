package com.zenobase.tasks.withings;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.elasticsearch.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class WithingsStepsResult {

	public static final Resource SOURCE = new Resource("Withings", "http://withings.com/");

	private final ObjectNode node;
	private final Identity author;
	private final String tag;
	private final Unit<Length> distanceUnit, heightUnit;

	public WithingsStepsResult(ObjectNode node, Identity author, String tag, Unit<Length> distanceUnit, Unit<Length> heightUnit) {
		this.node = node;
		this.author = author;
		this.tag = tag;
		this.distanceUnit = distanceUnit;
		this.heightUnit = heightUnit;
	}

	public int getStatus() {
		return node.path("status").isInt() ? node.path("status").intValue() : -1;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode activityNode : node.path("body").path("activities")) {
			events.add(getEvent((ObjectNode) activityNode));
		}
		return events;
	}

	private Event getEvent(ObjectNode node) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		DateTimeZone timezone = DateTimeZone.forID(node.path("timezone").textValue());
		DateTime time = dateTimeValue(node.path("date"), timezone);
		event.setValue(Event.TIMESTAMP, time);
		event.setValue(Event.DURATION, Period.days(1).toDurationFrom(time));
		event.setValue(Event.COUNT, node.path("steps").intValue());
		event.setValue(Event.ENERGY, measureValue(node.path("calories"), Measures.<Energy>parseUnit("cal")));
		event.setValue(Event.DISTANCE, convertMeasureValue(node.path("distance"), distanceUnit));
		event.setValue(Event.HEIGHT, convertMeasureValue(node.path("elevation"), heightUnit));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private static DateTime dateTimeValue(JsonNode node, DateTimeZone timezone) {
		LocalDate date = LocalDate.parse(node.textValue());
		return date.toDateTimeAtStartOfDay(timezone);
	}

	private static <Q extends Quantity> DecimalMeasure<Q> measureValue(JsonNode node, Unit<Q> unit) {
		return DecimalMeasure.valueOf(node.decimalValue(), unit);
	}

	private static <Q extends Quantity> DecimalMeasure<Q> convertMeasureValue(JsonNode node, Unit<Q> unit) {
		return node.isNumber() ? Measures.<Q>valueOf(Measures.convert(node.doubleValue(), unit), unit) : null;
	}
}
