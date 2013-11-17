package com.zenobase.tasks.bodymedia;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.measure.quantity.Energy;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class BodyMediaBurnResult extends BodyMediaResultSupport {

	static final String TAG = "burn";

	private final LocalDate date;
	private final TimezoneMap timezones;

	public BodyMediaBurnResult(ObjectNode node, Identity author, TimezoneMap timezones) {
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
		for (Map.Entry<DateTime, BigDecimal> entry : getCaloriesByHour(dayNode).entrySet()) {
			events.add(newEvent(entry.getKey(), entry.getValue()));
		}
		return events;
	}

	private Map<DateTime, BigDecimal> getCaloriesByHour(JsonNode dayNode) {
		Map<DateTime, BigDecimal> calories = Maps.newLinkedHashMap();
		DateTime time = Preconditions.checkNotNull(timezones.getBegin(date));
		for (JsonNode minuteNode : dayNode.path("minutes")) {
			if (minuteNode.path("source").textValue().equals("X")) {
				calories.clear(); // incomplete day
				break;
			}
			BigDecimal value = minuteNode.path("cals").decimalValue();
			DateTime hour = timezones.rezone(time).withMinuteOfHour(0);
			calories.put(hour, Objects.firstNonNull(calories.get(hour), BigDecimal.ZERO).add(value));
			time = time.plusMinutes(1);
		}
		return calories;
	}

	private Event newEvent(DateTime timestamp, BigDecimal calories) {
		Event event = new Event();
		event.setValue(Event.TAG, TAG);
		event.setValue(Event.TIMESTAMP, timestamp);
		event.setValue(Event.DURATION, Duration.standardHours(1));
		event.setValue(Event.ENERGY, Measures.<Energy>valueOf(calories, "cal"));
		event.setValue(Event.AUTHOR, getAuthor());
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
