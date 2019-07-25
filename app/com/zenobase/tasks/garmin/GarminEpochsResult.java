package com.zenobase.tasks.garmin;

import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class GarminEpochsResult extends GarminResultSupport {

	private final String tag;

	public GarminEpochsResult(JsonNode node, Identity author, String tag) {
		super(node, author);
		this.tag = tag;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode epochNode : node.path("epochs")) {
			events.add(newEvent(epochNode));
		}
		Map<DateTime, Event> hours = Maps.newTreeMap();
		for (Event event : events) {
			DateTime hour = event.getValue(Event.TIMESTAMP);
			if (hours.containsKey(hour)) {
				hours.replace(hour, merge(hours.get(hour), event));
			} else {
				hours.put(hour, event);
			}
		}
		return ImmutableList.copyOf(hours.values());
	}

	private Event newEvent(JsonNode node) {
		Event event = new Event();
		System.err.println("epoch: " + startTimeValue(node));
		event.addValue(Event.TAG, node.path("activityType").asText().toLowerCase());
		event.setValue(Event.TIMESTAMP, startTimeValue(node).withMinuteOfHour(0));
		event.setValue(Event.DURATION, durationValue(node.path("durationInSeconds")));
		event.setValue(Event.COUNT, node.path("steps").intValue());
		event.setValue(Event.ENERGY, Measures.valueOf(node.path("activeKilocalories").decimalValue(), Units.KCAL));
		event.setValue(Event.DISTANCE, Measures.valueOf(node.path("distanceInMeters").decimalValue().setScale(0, RoundingMode.HALF_UP), Units.M)); // TODO convert if imperial
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private Event merge(Event left, Event right) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, left.getValue(Event.TIMESTAMP));
		event.setValue(Event.DURATION, Duration.standardHours(1));
		event.setValue(Event.ENERGY, add(left.getValue(Event.ENERGY), right.getValue(Event.ENERGY)));
		event.setValue(Event.DISTANCE, add(left.getValue(Event.DISTANCE), right.getValue(Event.DISTANCE)));
		event.setValue(Event.COUNT, left.getValue(Event.COUNT) + right.getValue(Event.COUNT));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private static <Q extends Quantity> DecimalMeasure<Q> add(DecimalMeasure<Q> left, DecimalMeasure<Q> right) {
		Preconditions.checkArgument(left.getUnit().equals(right.getUnit()), "units must match");
		return DecimalMeasure.valueOf(left.getValue().add(right.getValue()), left.getUnit());
	}
}
