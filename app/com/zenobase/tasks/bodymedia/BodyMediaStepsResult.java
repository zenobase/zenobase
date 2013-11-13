package com.zenobase.tasks.bodymedia;

import java.util.List;
import java.util.Map;

import org.elasticsearch.common.base.Objects;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class BodyMediaStepsResult extends BodyMediaResultSupport {

	static final String TAG = "steps";

	private final LocalDate date;
	private final TimezoneMap timezones;

	public BodyMediaStepsResult(ObjectNode node, Identity author, TimezoneMap timezones) {
		super(node, author);
		this.date = getLocalDate(node.path("startDate"));
		this.timezones = timezones;
	}

	public LocalDate getDate() {
		return date;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		JsonNode dayNode = Iterables.getOnlyElement(path("days"));
		for (Map.Entry<DateTime, Integer> entry : getStepsByHour(dayNode).entrySet()) {
			events.add(newEvent(entry.getKey(), entry.getValue()));
		}
		return events;
	}

	private Map<DateTime, Integer> getStepsByHour(JsonNode dayNode) {
		Map<DateTime, Integer> steps = Maps.newLinkedHashMap();
		DateTime time = Preconditions.checkNotNull(timezones.getBegin(date));
		for (JsonNode hourNode : dayNode.path("hours")) {
			if (getLastSyncDate().isBefore(time)) {
				steps.clear();
				break;
			}
			DateTime hour = timezones.rezone(time);
			steps.put(hour, Objects.firstNonNull(steps.get(hour), 0) + hourNode.path("totalSteps").intValue());
			time = time.plusHours(1);
		}
		return steps;
	}

	private Event newEvent(DateTime timestamp, Integer steps) {
		Event event = new Event();
		event.setValue(Event.TAG, TAG);
		event.setValue(Event.TIMESTAMP, timestamp);
		event.setValue(Event.DURATION, Duration.standardHours(1));
		event.setValue(Event.COUNT, steps);
		event.setValue(Event.AUTHOR, getAuthor());
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
