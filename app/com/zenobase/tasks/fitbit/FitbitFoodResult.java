package com.zenobase.tasks.fitbit;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class FitbitFoodResult {

	public static final Resource SOURCE = new Resource("Fitbit", "http://fitbit.com/");
	private static final Unit<Energy> KCAL = Measures.parseUnit("kcal");

	private final JsonNode node;
	private final String tag;
	private final Identity author;
	private final DateTimeZone timezone;

	public FitbitFoodResult(JsonNode node, String tag, Identity author, DateTimeZone timezone) {
		this.node = node;
		this.tag = tag;
		this.author = author;
		this.timezone = timezone;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode foodNode : node.path("foods-log-caloriesIn")) {
			DecimalMeasure<Energy> value = kcalValue(foodNode.path("value"));
			if (value != null) {
				LocalDate date = LocalDate.parse(foodNode.path("dateTime").textValue());
				DateTime begin = date.toDateTimeAtStartOfDay(timezone);
				Event event = new Event();
				event.setValue(Event.TAG, tag);
				event.setValue(Event.TIMESTAMP, begin);
				event.setValue(Event.DURATION, Period.days(1).toDurationFrom(begin));
				event.setValue(Event.ENERGY, value);
				event.setValue(Event.AUTHOR, author);
				event.setValue(Event.SOURCE, SOURCE);
				events.add(event);
			}
		}
		return events;
	}

	private DecimalMeasure<Energy> kcalValue(JsonNode node) {
		return node.asInt() > 0 ? DecimalMeasure.valueOf(new BigDecimal(node.asInt()), KCAL) : null;
	}
}
