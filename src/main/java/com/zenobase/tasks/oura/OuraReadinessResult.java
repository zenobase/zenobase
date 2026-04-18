package com.zenobase.tasks.oura;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

class OuraReadinessResult extends OuraResultSupport {

	private final @Nullable String tag;
	private final DateTimeZone zone;

	public OuraReadinessResult(JsonNode node, Identity author, @Nullable String tag, DateTimeZone zone) {
		super(node, author);
		this.tag = tag;
		this.zone = zone;
	}

	@Override
	protected Event newEvent(JsonNode node) {
		var event = new Event();
		DateTime t = dateValue(node.path("day")).toDateTimeAtStartOfDay(zone);
		event.addValue(Event.TAG, tag);
		event.setValues(Event.TIMESTAMP, List.of(t, t.plusDays(1)));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
