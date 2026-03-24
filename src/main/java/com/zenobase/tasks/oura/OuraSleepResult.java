package com.zenobase.tasks.oura;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.joda.time.Duration;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class OuraSleepResult extends OuraResultSupport {

	private final String tag;

	public OuraSleepResult(JsonNode node, Identity author, String tag) {
		super(node, author);
		this.tag = tag;
	}

	@Override
	protected Event newEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValues(Event.TIMESTAMP, ImmutableList.of(dateTimeValue(node.path("bedtime_start")), dateTimeValue(node.path("bedtime_end"))));
		event.setValue(Event.DURATION, Duration.standardSeconds(intValue(node.path("total_sleep_duration"))));
		event.setValue(Event.FREQUENCY, frequencyValue(node.path("average_heart_rate")));
		event.setValue(Event.PERCENTAGE, percentageValue(node.path("efficiency")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
