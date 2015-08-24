package com.zenobase.tasks.misfit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.Period;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class MisfitStepsResult extends MisfitResultSupport {

	private final String tag;
	private final DateTimeZone zone;

	public MisfitStepsResult(JsonNode node, Identity author, String tag, DateTimeZone zone) {
		super("summary", node, author);
		this.tag = Preconditions.checkNotNull(tag);
		this.zone = Preconditions.checkNotNull(zone);
	}

	@Override
	protected Event newEvent(JsonNode node) {
		Event event = null;
		LocalDate date = dateValue(node.path("date"));
		DateTime t = date.toDateTimeAtStartOfDay(zone);
		event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, t);
		event.setValue(Event.DURATION, new Duration(t, t.plus(Period.days(1))));
		event.setValue(Event.COUNT, intValue(node.path("steps")));
		event.setValue(Event.DISTANCE, distanceValue(node.path("distance")));
		event.setValue(Event.ENERGY, energyValue(node.path("calories")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
