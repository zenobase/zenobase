package com.zenobase.tasks.withings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;

class WithingsSleepResult extends WithingsResult {

	private static final Logger logger = LoggerFactory.getLogger(WithingsSleepResult.class);

	private final List<Event> events = new ArrayList<>();
	private final boolean useRanges;
	private final DateTimeZone timezone;

	public WithingsSleepResult(
			ObjectNode node, Identity author, @Nullable String tag, boolean useRanges, DateTimeZone timezone) {
		super(node, author, tag);
		this.useRanges = useRanges;
		this.timezone = timezone;
	}

	public WithingsSleepResult(
			List<Event> events, Identity author, @Nullable String tag, boolean useRanges, DateTimeZone timezone) {
		this(Nodes.newObject(), author, tag, useRanges, timezone);
		this.events.addAll(events);
	}

	public List<Event> getEvents() {
		if (events.isEmpty()) {
			for (JsonNode seriesNode : node.path("body").path("series")) {
				Event event = getEvent(seriesNode);
				if (event != null) {
					events.add(event);
				}
			}
		}
		return events;
	}

	private @Nullable Event getEvent(JsonNode node) {
		DateTime begin = dateTimeValue(node.path("startdate"), timezone);
		DateTime end = dateTimeValue(node.path("enddate"), timezone);
		if (begin == null || end == null) {
			logger.warn("Missing a start or end date: {}", node);
			return null;
		}
		var event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, begin);
		if (useRanges) {
			event.addValue(Event.TIMESTAMP, end);
		}
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.setValue(Event.PERCENTAGE, percentageValue(node.path("state")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private static @Nullable DateTime dateTimeValue(JsonNode node, DateTimeZone timezone) {
		return !node.isMissingNode() ? new DateTime(node.longValue() * 1000L, timezone) : null;
	}

	private static Percentage percentageValue(JsonNode node) {
		return Percentage.valueOf(node.intValue() > 0 ? 100 : 0);
	}

	public void add(List<Event> events) {
		getEvents().addAll(events);
	}

	public WithingsSleepResult merge() {
		List<Event> merged = new ArrayList<>();
		Event prev = null;
		DateTime end = null;
		for (Event event : getEvents()) {
			DateTime begin = getBegin(event);
			Duration duration = event.getValue(Event.DURATION);
			if (prev != null && end != null && end.equals(begin)) {
				prev.setValue(Event.PERCENTAGE, meanPercentage(prev, event));
				prev.setValue(
						Event.DURATION,
						Objects.requireNonNull(prev.getValue(Event.DURATION)).plus(duration));
				end = getEnd(event);
				prev.setValues(Event.TIMESTAMP, List.of(getBegin(prev), end));
				continue;
			}
			merged.add(event);
			prev = event;
			end = getEnd(event);
		}
		return new WithingsSleepResult(merged, author, tag, useRanges, timezone);
	}

	private static @Nullable DateTime getBegin(Event event) {
		return Ordering.natural().min(event.getValues(Event.TIMESTAMP));
	}

	private static @Nullable DateTime getEnd(Event event) {
		return Ordering.natural().max(event.getValues(Event.TIMESTAMP));
	}

	@Override
	public @Nullable String getMarker() {
		return !getEvents().isEmpty()
				? Objects.requireNonNull(Ordering.natural()
								.max(Objects.requireNonNull(Iterables.getLast(getEvents()))
										.getValues(Event.TIMESTAMP)))
						.toString()
				: null;
	}

	private static Percentage meanPercentage(Event left, Event right) {
		return mean(
				Objects.requireNonNull(left.getValue(Event.PERCENTAGE)),
				Ints.checkedCast(
						Objects.requireNonNull(left.getValue(Event.DURATION)).getStandardSeconds()),
				Objects.requireNonNull(right.getValue(Event.PERCENTAGE)),
				Ints.checkedCast(
						Objects.requireNonNull(right.getValue(Event.DURATION)).getStandardSeconds()));
	}

	private static Percentage mean(Percentage left, int leftWeight, Percentage right, int rightWeight) {
		return Percentage.valueOf(
				(left.value().intValue() * leftWeight + right.value().intValue() * rightWeight)
						/ (leftWeight + rightWeight));
	}
}
