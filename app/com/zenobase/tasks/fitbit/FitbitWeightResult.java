package com.zenobase.tasks.fitbit;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Resource;

class FitbitWeightResult {

	public static final Resource SOURCE = new Resource("Fitbit", "http://fitbit.com/");

	private final JsonNode node;
	private final String tag;
	private final Identity author;
	private final LocalDate date;
	private final DateTimeZone timezone;
	private final Unit<Mass> weightUnit;

	public FitbitWeightResult(JsonNode node, String tag, Identity author, LocalDate date, DateTimeZone timezone, Unit<Mass> weightUnit) {
		this.node = node;
		this.tag = tag;
		this.author = author;
		this.date = date;
		this.timezone = timezone;
		this.weightUnit = weightUnit;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		DecimalMeasure<Mass> weight = getWeight(node.path("body").path("weight"));
		if (weight != null) {
			DateTime begin = date.toDateTimeAtStartOfDay(timezone);
			Event event = new Event();
			event.setValue(Event.TAG, tag);
			event.setValue(Event.TIMESTAMP, begin);
			event.setValue(Event.WEIGHT,  weight);
			event.setValue(Event.PERCENTAGE, getPercentage(node.path("body").path("fat")));
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
			events.add(event);
		}
		return events;
	}

	private static Percentage getPercentage(JsonNode node) {
		int value = node.intValue();
		return value > 0 ? Percentage.valueOf(value) : null;
	}

	private DecimalMeasure<Mass> getWeight(JsonNode node) {
		int value = node.intValue();
		return value > 0 ? Measures.valueOf(node.decimalValue(), weightUnit) : null;
	}
}
