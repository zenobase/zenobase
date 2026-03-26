package com.zenobase.tasks.ihealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class IHealthSleepResult extends IHealthResultSupport {

	private final String tag;
	private final DateTimeZone zone;

	public IHealthSleepResult(JsonNode node, Identity author, @Nullable String tag, DateTimeZone zone) {
		super("SRDataList", node, author);
		this.tag = Preconditions.checkNotNull(tag);
		this.zone = Preconditions.checkNotNull(zone);
	}

	@Override
	protected Event newEvent(JsonNode node) {
		var event = new Event();
		event.addValue(Event.TAG, tag);
		event.addValue(Event.TIMESTAMP, dateTimeValue(node.path("StartTime"), zone));
		event.addValue(Event.TIMESTAMP, dateTimeValue(node.path("EndTime"), zone));
		event.setValue(Event.DURATION, durationValue(node.path("HoursSlept")));
		event.setValue(Event.PERCENTAGE, percentageValue(node.path("SleepEfficiency")));
		event.setValue(Event.LOCATION, locationValue(node));
		event.setValue(Event.NOTE, textValue(node.path("Note")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private @Nullable Duration durationValue(JsonNode node) {
		double hours = node.doubleValue();
		return hours > 0.0 ? Duration.standardMinutes(Math.round(hours * 60.0)) : null;
	}
}
