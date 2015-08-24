package com.zenobase.tasks.jawbone;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class FoodResult extends JawboneResult {

	public FoodResult(JsonNode node, Identity author, String tag) {
		super(node, author, tag);
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode foodNode : node.path("items")) {
			Event event = newEvent(foodNode);
			if (event != null) {
				events.add(event);
			}
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		Event event = null;
		DateTime begin = dateTimeValue(node.path("time_created"), dateTimeZoneValue(node.path("details").path("tz")));
		DecimalMeasure<Energy> value = energyValue(node.path("details").path("calories"));
		if (value != null && BigDecimal.ZERO.compareTo(value.getValue()) < 0) {
			event = new Event();
			event.addValue(Event.TAG, tag);
			event.setValue(Event.TIMESTAMP, begin);
			event.setValue(Event.ENERGY, value);
			event.setValue(Event.LOCATION, locationValue(node));
			event.setValue(Event.SOURCE, SOURCE);
			event.setValue(Event.AUTHOR, author);
		}
		return event;
	}
}
