package com.zenobase.tasks.jawbone;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Mass;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;

import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class WeightResult extends JawboneResult {

	private final boolean metric;

	public WeightResult(JsonNode node, Identity author, String tag, boolean metric) {
		super(node, author, tag);
		this.metric = metric;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode weightNode : node.path("items")) {
			Event event = newEvent(weightNode);
			if (event != null) {
				events.add(event);
			}
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		Event event = null;
		DateTime begin = dateTimeValue(node.path("time_created"), dateTimeZoneValue(node.path("details").path("tz")));
		DecimalMeasure<Mass> value = weightValue(node.path("weight"), metric ? Units.KG : Units.LB);
		if (value != null && BigDecimal.ZERO.compareTo(value.getValue()) < 0) {
			event = new Event();
			event.addValue(Event.TAG, tag);
			event.setValue(Event.TIMESTAMP, begin);
			event.setValue(Event.WEIGHT, value);
			event.setValue(Event.PERCENTAGE, percentageValue(node.path("body_fat")));
			event.setValue(Event.LOCATION, locationValue(node));
			event.setValue(Event.SOURCE, SOURCE);
			event.setValue(Event.AUTHOR, author);
		}
		return event;
	}
}
