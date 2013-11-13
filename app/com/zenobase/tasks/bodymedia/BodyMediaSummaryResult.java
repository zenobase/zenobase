package com.zenobase.tasks.bodymedia;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.measure.quantity.Energy;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Rating;

class BodyMediaSummaryResult extends BodyMediaResultSupport {

	static final String TAG_SLEEP = "sleep";
	static final String TAG_STEPS = "steps";

	public BodyMediaSummaryResult(ObjectNode node, Identity author) {
		super(node, author);
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode dayNode : path("sleep").path("days")) {
			addSleep(dayNode, events);
		}
		Map<LocalDate, BigDecimal> burn = getBurn();
		for (JsonNode dayNode : path("step").path("days")) {
			addSteps(dayNode, burn, events);
		}
		return events;
	}

	private void addSleep(JsonNode node, List<Event> events) {
		LocalDate date = getLocalDate(node.path("date"));
		if (getLastSyncDate().toLocalDate().isAfter(date)) {
			Event event = new Event();
			event.setValue(Event.TAG, TAG_SLEEP);
			event.setValue(Event.TIMESTAMP, toDateTime(date, getLastSyncDate().getZone()));
			event.setValue(Event.DURATION, Duration.standardMinutes(node.path("totalLying").intValue()));
			event.setValue(Event.RATING, Rating.valueOf(node.path("efficiency").decimalValue().scaleByPowerOfTen(2).intValue()));
			event.setValue(Event.AUTHOR, getAuthor());
			event.setValue(Event.SOURCE, SOURCE);
			events.add(event);
		}
	}

	private Map<LocalDate, BigDecimal> getBurn() {
		Map<LocalDate, BigDecimal> burn = Maps.newHashMap();
		for (JsonNode dayNode : path("burn").path("days")) {
			LocalDate date = getLocalDate(dayNode.path("date"));
			BigDecimal value = getBigDecimal(dayNode.path("totalCalories"));
			if (!value.equals(BigDecimal.ZERO)) {
				burn.put(date, value);
			}
		}
		return burn;
	}

	private void addSteps(JsonNode stepNode, Map<LocalDate, BigDecimal> burn, List<Event> events) {
		LocalDate date = getLocalDate(stepNode.path("date"));
		if (getLastSyncDate().toLocalDate().isAfter(date)) {
			Event event = new Event();
			event.setValue(Event.TAG, TAG_STEPS);
			event.setValue(Event.TIMESTAMP, toDateTime(date, getLastSyncDate().getZone()));
			event.setValue(Event.COUNT, stepNode.path("totalSteps").intValue());
			if (burn.containsKey(date)) {
				event.setValue(Event.ENERGY, Measures.<Energy>valueOf(burn.get(date).negate(), "cal"));
			}
			event.setValue(Event.AUTHOR, getAuthor());
			event.setValue(Event.SOURCE, SOURCE);
			events.add(event);
		}
	}

	private static DateTime toDateTime(LocalDate date, DateTimeZone zone) {
		return date.toDateTime(new LocalTime(12, 0), zone);
	}

	private static BigDecimal getBigDecimal(JsonNode node) {
		return BigDecimal.valueOf(node.longValue());
	}
}
