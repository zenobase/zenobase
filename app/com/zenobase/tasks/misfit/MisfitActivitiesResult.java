package com.zenobase.tasks.misfit;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTime;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class MisfitActivitiesResult extends MisfitResultSupport {

	static final Resource SOURCE = new Resource("Misfit", "http://misfit.com/");

	private final DateTime begin;

	public MisfitActivitiesResult(JsonNode node, Identity author, DateTime begin) {
		super("sessions", node, author);
		this.begin = begin;
	}

	@Override
	protected Event newEvent(JsonNode node) {
		Event event = null;
		DateTime t = dateTimeValue(node.path("startTime"));
		if (t.isAfter(begin)) {
			event = new Event();
			event.addValue(Event.TAG, node.path("activityType").textValue());
			event.setValue(Event.TIMESTAMP, t);
			event.setValue(Event.DURATION, durationValue(node.path("duration")));
			event.setValue(Event.COUNT, intValue(node.path("steps")));
			event.setValue(Event.DISTANCE, distanceValue(node.path("distance")));
			event.setValue(Event.ENERGY, energyValue(node.path("calories")));
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
		}
		return event;
	}
}
