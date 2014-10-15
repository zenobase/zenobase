package com.zenobase.tasks.bodymedia;

import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class BodyMediaStepsResult extends BodyMediaResultSupport {

	private final String tag;
	private final boolean hourly;
	private final LocalDate date;
	private final TimezoneMap timezones;

	public BodyMediaStepsResult(ObjectNode node, Identity author, String tag, boolean hourly, TimezoneMap timezones) {
		super(node, author);
		this.tag = tag;
		this.hourly = hourly;
		this.date = getLocalDate(node.path("startDate"));
		this.timezones = timezones;
	}

	public LocalDate getDate() {
		return date;
	}

	public List<Event> getEvents() {
		JsonNode daysNode = path("days");
		return daysNode.size() > 0
			? getEvents(Iterables.getOnlyElement(daysNode))
			: Collections.<Event>emptyList();
	}

	public List<Event> getEvents(JsonNode dayNode) {
		List<Event> events = Lists.newArrayList();
		DateTime time = Preconditions.checkNotNull(timezones.getBegin(date));
		if (hourly) {
			for (JsonNode hourNode : dayNode.path("hours")) {
				if (getLastSyncDate().isBefore(time)) {
					return Collections.emptyList();
				}
				DateTime hour = timezones.rezone(time);
				events.add(newEvent(hour, hourNode.path("totalSteps").intValue(), 1));
				time = time.plusHours(1);
			}
		} else {
			events.add(newEvent(timezones.rezone(time), dayNode.path("totalSteps").intValue(), dayNode.path("hours").size()));
		}
		return events;
	}

	private Event newEvent(DateTime timestamp, Integer steps, int hours) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, timestamp);
		event.setValue(Event.DURATION, Duration.standardHours(hours));
		event.setValue(Event.COUNT, steps);
		event.setValue(Event.AUTHOR, getAuthor());
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
