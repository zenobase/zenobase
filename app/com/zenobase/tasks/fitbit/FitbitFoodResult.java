package com.zenobase.tasks.fitbit;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class FitbitFoodResult extends FitbitResultSupport {

	public FitbitFoodResult(JsonNode node, String tag, Identity author, DateTimeZone timezone) {
		super(node, tag, author, timezone);
	}

	@Override
	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode foodNode : node.path("foods-log-caloriesIn")) {
			DecimalMeasure<Energy> value = energyValue(foodNode.path("value"), Units.KCAL);
			if (!BigDecimal.ZERO.equals(value.getValue())) {
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
}
