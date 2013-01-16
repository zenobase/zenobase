package com.zenobase.tasks.fitbit;

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;

class FitbitSleepResult {


	public static final Resource SOURCE = new Resource("Fitbit", "http://fitbit.com/");

	private final String tag = "sleeping";
	private final JsonNode node;
	private final Identity author;
	private final DateTimeZone timezone;

	public FitbitSleepResult(JsonNode node, Identity author, DateTimeZone timezone) {
		this.node = node;
		this.author = author;
		this.timezone = timezone;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode item : node.path("sleep")) {
			Event event = new Event();
			event.setValue(Event.TAG, tag);
			event.setValue(Event.TIMESTAMP, getDateTime(item, timezone));
			event.setValue(Event.DURATION, getDuration(item));
			event.setValue(Event.RATING, getRating(item));
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
			events.add(event);
		}
		return events;
	}

	private static DateTime getDateTime(JsonNode item, DateTimeZone timezone) {
		String value = item.path("startTime").getTextValue();
		Preconditions.checkNotNull(value, "Missing sleep start time");
		return LocalDateTime.parse(value).toDateTime(timezone);
	}

	private static Duration getDuration(JsonNode item) {
		long value = item.path("duration").getLongValue();
		return value > 0 ? Duration.millis(value) : null;
	}

	private static Rating getRating(JsonNode item) {
		int value = item.path("efficiency").getIntValue();
		return value > 0 ? Rating.valueOf(value) : null;
	}
}
