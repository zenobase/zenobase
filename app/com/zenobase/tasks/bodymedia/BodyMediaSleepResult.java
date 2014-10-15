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
import com.google.common.primitives.Ints;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Rating;

class BodyMediaSleepResult extends BodyMediaResultSupport {

	private final String tag;
	private final boolean useRanges;
	private final LocalDate date;
	private final TimezoneMap timezones;

	public BodyMediaSleepResult(ObjectNode node, Identity author, String tag, boolean useRanges, TimezoneMap timezones) {
		super(node, author);
		this.tag = tag;
		this.useRanges = useRanges;
		this.date = getLocalDate(node.path("startDate"));
		this.timezones = timezones;
	}

	public LocalDate getDate() {
		return date;
	}

	public List<Event> getEvents() {
		JsonNode dayNode = path("days");
		return dayNode.size() > 0
			? getEvents(Iterables.getOnlyElement(dayNode))
			: Collections.<Event>emptyList();
	}

	private List<Event> getEvents(JsonNode dayNode) {
		List<Event> events = Lists.newArrayList();
		DateTime noon = Preconditions.checkNotNull(timezones.getBegin(date).withHourOfDay(12).minusDays(1));
		DateTime begin = null;
		int duration = 0;
		int sleeping = 0;
		for (JsonNode periodNode : dayNode.path("sleepPeriods")) {
			int offset = intValue(periodNode.path("minuteIndex"));
			DateTime time = timezones.rezone(noon.plusMinutes(offset));
			if (begin == null) {
				begin = time;
			}
			int d = intValue(periodNode.path("duration"));
			if (duration > 0 && begin.plusMinutes(duration).isBefore(time)) {
				events.add(newEvent(begin, duration, sleeping));
				begin = time;
				duration = 0;
				sleeping = 0;
			}
			duration += d;
			if ("ASLEEP".equals(textValue(periodNode.path("state")))) {
				sleeping += d;
			}
		}
		if (duration > 0) {
			events.add(newEvent(begin, duration, sleeping));
		}
		return events;
	}

	private int intValue(JsonNode node) {
		Preconditions.checkState(node.isInt());
		return node.intValue();
	}

	private String textValue(JsonNode node) {
		Preconditions.checkState(node.isTextual());
		return node.textValue();
	}

	private Event newEvent(DateTime timestamp, int minutesTotal, int minutesSleeping) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, timestamp);
		if (useRanges) {
			event.addValue(Event.TIMESTAMP, timestamp.plusMinutes(minutesTotal));
		}
		event.setValue(Event.DURATION, Duration.standardMinutes(minutesTotal));
		event.setValue(Event.RATING, Rating.valueOf(minutesSleeping > 0 ? Ints.checkedCast(Math.round(100.0 * minutesSleeping / minutesTotal)) : 0));
		event.setValue(Event.AUTHOR, getAuthor());
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
