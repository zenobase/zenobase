package com.zenobase.tasks.ihealth;

import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class IHealthBloodOxygenResult extends IHealthResultSupport {

	private final String tag;
	private final DateTimeZone zone;

	public IHealthBloodOxygenResult(JsonNode node, Identity author, String tag, DateTimeZone zone) {
		super("BODataList", node, author);
		this.tag = Preconditions.checkNotNull(tag);
		this.zone = Preconditions.checkNotNull(zone);
	}

	@Override
	protected Event newEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("MDate"), zone));
		event.setValue(Event.PERCENTAGE, percentageValue(node.path("BO")));
		event.setValue(Event.FREQUENCY, frequencyValue(node.path("HR")));
		event.setValue(Event.LOCATION, locationValue(node));
		event.setValue(Event.NOTE, textValue(node.path("Note")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
