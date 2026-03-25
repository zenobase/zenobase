package com.zenobase.tasks.rescuetime;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.IllegalInstantException;
import org.joda.time.LocalDateTime;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;

public class ProductivityResult {

	public static final Resource SOURCE = new Resource("RescueTime", "https://www.rescuetime.com/");

	private final ObjectNode node;
	private final Identity author;
	private final String tag;
	private final DateTimeZone timezone;

	public ProductivityResult(ObjectNode node, Identity author, String tag, DateTimeZone timezone) {
		this.node = node;
		this.author = author;
		this.tag = tag;
		this.timezone = timezone;
	}

	public boolean isSuccess() {
		return node.path("error").isMissingNode();
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode hourNode : node.path("rows")) {
			try {
				events.add(getEvent(hourNode));
			} catch (IllegalInstantException e) {

			}
		}
		return events;
	}

	private Event getEvent(JsonNode node) {
		var event = new Event();
		if (tag != null) {
			event.addValue(Event.TAG, tag);
		}
		event.setValue(Event.TIMESTAMP, timeValue(node.get(0)));
		event.setValue(Event.DURATION, Duration.standardSeconds(node.get(1).intValue()));
		if (node.path(3).isTextual()) {
			event.addValue(Event.TAG, node.path(3).textValue());
		}
		if (node.path(4).isNumber()) {
			event.setValue(Event.RATING, Rating.valueOf(Ints.checkedCast(Math.round(node.get(4).doubleValue()))));
		}
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private DateTime timeValue(JsonNode node) {
		return LocalDateTime.parse(node.textValue()).toDateTime(timezone);
	}
}
