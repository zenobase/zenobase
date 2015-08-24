package com.zenobase.tasks.fitbit;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class FitbitSleepResult extends FitbitResultSupport {

	private final boolean useRanges;

	public FitbitSleepResult(JsonNode node, String tag, Identity author, boolean useRanges, DateTimeZone timezone) {
		super(node, tag, author, timezone);
		this.useRanges = useRanges;
	}

	@Override
	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode item : node.path("sleep")) {
			DateTime begin = dateTimeValue(item.path("startTime"));
			Duration duration = durationValue(item.path("duration"));
			Event event = new Event();
			event.setValue(Event.TAG, tag);
			event.setValue(Event.TIMESTAMP, begin);
			if (useRanges) {
				event.addValue(Event.TIMESTAMP, begin.plus(duration));
			}
			event.setValue(Event.DURATION, duration);
			event.setValue(Event.RATING, ratingValue(item.path("efficiency")));
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
			events.add(event);
		}
		return events;
	}
}
