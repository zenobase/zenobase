package com.zenobase.tasks.garmin;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class GarminActivitiesResult extends GarminResultSupport {

	public GarminActivitiesResult(JsonNode node, Identity author) {
		super(node, author);
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode activityNode : node.path("activities")) {
			events.add(newEvent(activityNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TAG, node.path("activityType").asText().toLowerCase());
		event.setValue(Event.TIMESTAMP, startTimeValue(node));
		event.setValue(Event.DURATION, durationValue(node.path("durationInSeconds")));
		event.setValue(Event.ENERGY, energyValue(node.path("activeKilocalories")));
		event.setValue(Event.FREQUENCY, frequencyValue(node.path("averageHeartRateInBeatsPerMinute")));
		event.setValue(Event.COUNT, intValue(node.path("steps")));
		event.setValue(Event.DISTANCE, lengthValue(node.path("distanceInMeters"))); // TODO convert if imperial
		event.setValue(Event.HEIGHT, lengthValue(node.path("totalElevationGainInMeters"))); // TODO convert if imperial
		event.setValue(Event.LOCATION, locationValue(node.path("startingLatitudeInDegree"), node.path("startingLongitudeInDegree")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
