package com.zenobase.tasks.bodymedia;

import java.util.List;

import javax.measure.quantity.Energy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.common.collect.Iterables;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import play.Logger;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.RangeMap;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class BodyMediaBurnResult {

	static final String TAG = "burn";
	static final Resource SOURCE = new Resource("BodyMedia", "http://bodymedia.com/");

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("yyyyMMdd");

	private final ObjectNode node;
	private final Identity author;
	private final LocalDate date;
	private final RangeMap<LocalDateTime, DateTimeZone> timezones;

	public BodyMediaBurnResult(ObjectNode node, Identity author, RangeMap<LocalDateTime, DateTimeZone> timezones) {
		this.node = node;
		this.author = author;
		this.date = getLocalDate(node.path("startDate"));
		this.timezones = timezones;
	}

	public LocalDate getDate() {
		return date;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		addDay(Iterables.getOnlyElement(node.path("days")), events);
		return events;
	}

	private void addDay(JsonNode dayNode, List<Event> events) {
		LocalTime time = new LocalTime(0, 0);
		for (JsonNode minuteNode : dayNode.path("minutes")) {
			DateTimeZone timezone = timezones.get(date.toLocalDateTime(time));
			if (timezone == null) {
				Logger.warn("Can't find timzone for " + date.toLocalDateTime(time) + " in " + timezones);
				continue;
			}
			if (minuteNode.path("source").textValue().equals("X")) {
				events.clear(); // incomplete day
				return;
			}
			addMinute(minuteNode, date.toDateTime(time, timezone), events);
			time = time.plusMinutes(1);
		}
	}

	private void addMinute(JsonNode node, DateTime timestamp, List<Event> events) {
		Event event = new Event();
		event.setValue(Event.TAG, TAG);
		event.setValue(Event.TIMESTAMP, timestamp);
		event.setValue(Event.ENERGY, Measures.<Energy>valueOf(node.path("cals").decimalValue(), "cal"));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		events.add(event);
	}

	private static LocalDate getLocalDate(JsonNode node) {
		Preconditions.checkArgument(!node.isMissingNode());
		return LocalDate.parse(node.textValue(), DATE_FORMAT);
	}
}
