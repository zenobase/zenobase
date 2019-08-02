package com.zenobase.tasks.oura;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class OuraStepsResult extends OuraResultSupport {

	private final String tag;

	public OuraStepsResult(JsonNode node, Identity author, String tag) {
		super("activity", node, author);
		this.tag = tag;
	}

	@Override
	protected Event newEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValues(Event.TIMESTAMP, ImmutableList.of(dateTimeValue(node.path("day_start")), dateTimeValue(node.path("day_end"))));
		event.setValue(Event.COUNT, intValue(node.path("steps")));
		event.setValue(Event.ENERGY, energyValue(node.path("cal_total")));
		event.setValue(Event.RATING, ratingValue(node.path("score")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
