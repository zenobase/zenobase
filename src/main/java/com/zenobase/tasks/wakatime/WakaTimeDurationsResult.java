package com.zenobase.tasks.wakatime;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class WakaTimeDurationsResult {

	static final Resource SOURCE = new Resource("WakaTime", "https://wakatime.com/");

	private final JsonNode node;
	private final Identity author;
	private final @Nullable String tag;
	private final DateTimeZone zone;

	public WakaTimeDurationsResult(JsonNode node, Identity author, @Nullable String tag) {
		this.node = Preconditions.checkNotNull(node);
		this.author = Preconditions.checkNotNull(author);
		this.tag = tag;
		this.zone = DateTimeZone.forID(node.path("timezone").textValue());
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode dataNode : node.path("data")) {
			events.add(newEvent(dataNode));
		}
		return events;
	}

	public Event newEvent(JsonNode node) {
		var event = new Event();
		long t = node.path("time").decimalValue().movePointRight(3).longValue();
		long d = node.path("duration").decimalValue().movePointRight(3).longValue();
		Preconditions.checkState(t > 0);
		event.setValue(Event.TIMESTAMP, new DateTime(t, zone));
		event.setValue(Event.DURATION, Duration.millis(d));
		event.addValue(Event.TAG, tag);
		event.addValue(Event.TAG, node.path("project").textValue());
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
