package com.zenobase.tasks.fitbit;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class FitbitIntradayResult {

	public static final Resource SOURCE = new Resource("Fitbit", "http://fitbit.com/");

	private final JsonNode node;
	private final Identity author;
	private final LocalDate date;
	private final DateTimeZone timezone;
	private final List<Interval> ignore;
	private final int threshold = 1;

	public FitbitIntradayResult(JsonNode node, Identity author, LocalDate date, DateTimeZone timezone, List<Interval> ignore) {
		this.node = node;
		this.author = author;
		this.date = date;
		this.timezone = timezone;
		this.ignore = ignore;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		Record previous = null;
		for (JsonNode recordNode : node.path("activities-calories-intraday").path("dataset")) {
			Record current = getRecord(recordNode);
			boolean ignored = shouldIgnore(current);
			if (previous == null || !previous.extend(current) || ignored) {
				if (previous != null) {
					events.add(toEvent(previous));
				}
				previous = !ignored ? current : null;
			}
		}
		return events;
	}

	private boolean shouldIgnore(Record record) {
		for (Interval interval : ignore) {
			if (interval.contains(record.timestamp)) {
				return true;
			}
		}
		return false;
	}

	private Record getRecord(JsonNode node) {
		LocalTime time = LocalTime.parse(node.path("time").textValue());
		DateTime timestamp = date.toDateTime(time, timezone);
		int level = node.path("level").intValue();
		return new Record(timestamp, level >= threshold);
	}

	private Event toEvent(Record record) {
		Event event = new Event();
		event.setValue(Event.TAG, record.active ? "moving" : "sitting");
		event.setValue(Event.TIMESTAMP, record.timestamp);
		event.setValue(Event.DURATION, Duration.standardMinutes(record.minutes));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private static class Record {

		private final DateTime timestamp;
		private final boolean active;
		private int minutes = 1;

		public Record(DateTime timestamp, boolean active) {
			this.timestamp = timestamp;
			this.active = active;
		}

		public boolean extend(Record that) {
			if (that.active == active) {
				++minutes;
				return true;
			}
			return false;
		}

		@Override
		public String toString() {
			return timestamp + " +" + minutes + " <" + (active ? "moving" : "sitting") + ">";
		}
	}
}
