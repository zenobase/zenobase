package com.zenobase.tasks.oura;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class OuraSleepResult extends OuraResultSupport {

	private final @Nullable String tag;

	public OuraSleepResult(JsonNode node, Identity author, @Nullable String tag) {
		super(node, author);
		this.tag = tag;
	}

	@Override
	protected @Nullable Event newEvent(JsonNode node) {
		var event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValues(
			Event.TIMESTAMP,
			List.of(dateTimeValue(node.path("bedtime_start")), dateTimeValue(node.path("bedtime_end")))
		);
		event.setValue(Event.DURATION, durationValue(node.path("total_sleep_duration")));
		event.setValue(Event.FREQUENCY, frequencyValue(node.path("average_heart_rate")));
		event.setValue(Event.PERCENTAGE, percentageValue(node.path("efficiency")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
