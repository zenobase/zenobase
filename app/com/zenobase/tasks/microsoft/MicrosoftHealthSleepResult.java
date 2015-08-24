package com.zenobase.tasks.microsoft;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class MicrosoftHealthSleepResult extends MicrosoftHealthResultSupport {

	private final String tag;

	public MicrosoftHealthSleepResult(JsonNode node, Identity author, DateTimeZone zone, String tag) {
		super(node, author, zone);
		this.tag = tag;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode activityNode : node.path("sleepActivities")) {
			events.add(newEvent(activityNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		Event event = new Event();
		DateTime begin = dateTimeValue(node.path("startTime"));
		DateTime end = dateTimeValue(node.path("endTime"));
		event.addValue(Event.TIMESTAMP, begin);
		event.addValue(Event.TIMESTAMP, end);
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.setValue(Event.PERCENTAGE, percentageValue(node.path("sleepEfficiencyPercentage")));
		event.addValue(Event.TAG, tag);
		event.setValue(Event.FREQUENCY, frequencyValue(node.path("restingHeartRate")));
		event.setValue(Event.ENERGY, energyValue(node.path("caloriesBurnedSummary").path("totalCalories")));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}
}
