package com.zenobase.tasks.ihealth;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class IHealthActivitiesResult extends IHealthResultSupport {

	private final String tag;
	private final DateTimeZone zone;

	public IHealthActivitiesResult(JsonNode node, Identity author, String tag, DateTimeZone zone) {
		super("SPORTDataList", node, author);
		this.tag = Preconditions.checkNotNull(tag);
		this.zone = Preconditions.checkNotNull(zone);
	}

	@Override
	protected Event newEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		String name = textValue(node.path("SportName"));
		if (name != null) {
			event.addValue(Event.TAG, name);
		}
		DateTime t0 = dateTimeValue(node.path("SportStartTime"), zone);
		DateTime t1 = dateTimeValue(node.path("SportEndTime"), zone);
		event.setValue(Event.TIMESTAMP, t0);
		event.setValue(Event.DURATION, new Duration(t0, t1));
		event.setValue(Event.ENERGY, energyValue(node.path("Calories")));
		event.setValue(Event.LOCATION, locationValue(node));
		event.setValue(Event.NOTE, textValue(node.path("Note")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
