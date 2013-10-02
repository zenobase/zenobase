package com.zenobase.tasks.bodymedia;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.measure.quantity.Energy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;

class BodyMediaSummaryResult {

	static final String TAG_SLEEP = "sleep";
	static final String TAG_STEPS = "steps";
	static final Resource SOURCE = new Resource("BodyMedia", "http://bodymedia.com/");

	private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZZ").withOffsetParsed();
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("yyyyMMdd");

	private final ObjectNode node;
	private final Identity author;
	private final DateTime lastSync;

	public BodyMediaSummaryResult(ObjectNode node, Identity author) {
		this.node = node;
		this.author = author;
		this.lastSync = getDateTime(node.path("lastSync").path("dateTime"));
	}

	public LocalDate getLastSyncDate() {
		return lastSync.toLocalDate();
	}

	public DateTimeZone getTimezone() {
		String text = node.path("lastSync").path("dateTime").textValue();
		return text != null ? DateTime.parse(text, DATE_TIME_FORMAT).getZone() : null;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode dayNode : node.path("sleep").path("days")) {
			addSleep(dayNode, events);
		}
		Map<LocalDate, BigDecimal> burn = getBurn();
		for (JsonNode dayNode : node.path("step").path("days")) {
			addSteps(dayNode, burn, events);
		}
		return events;
	}

	private void addSleep(JsonNode node, List<Event> events) {
		LocalDate date = getLocalDate(node.path("date"));
		if (lastSync.toLocalDate().isAfter(date)) {
			Event event = new Event();
			event.setValue(Event.TAG, TAG_SLEEP);
			event.setValue(Event.TIMESTAMP, toDateTime(date, lastSync.getZone()));
			event.setValue(Event.DURATION, Duration.standardMinutes(node.path("totalLying").intValue()));
			event.setValue(Event.RATING, Rating.valueOf(node.path("efficiency").decimalValue().scaleByPowerOfTen(2).intValue()));
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
			events.add(event);
		}
	}

	private Map<LocalDate, BigDecimal> getBurn() {
		Map<LocalDate, BigDecimal> burn = Maps.newHashMap();
		for (JsonNode dayNode : node.path("burn").path("days")) {
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
		if (lastSync.toLocalDate().isAfter(date)) {
			Event event = new Event();
			event.setValue(Event.TAG, TAG_STEPS);
			event.setValue(Event.TIMESTAMP, toDateTime(date, lastSync.getZone()));
			event.setValue(Event.COUNT, stepNode.path("totalSteps").intValue());
			if (burn.containsKey(date)) {
				event.setValue(Event.ENERGY, Measures.<Energy>valueOf(burn.get(date).negate(), "cal"));
			}
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
			events.add(event);
		}
	}

	private static DateTime getDateTime(JsonNode node) {
		Preconditions.checkArgument(!node.isMissingNode());
		return DateTime.parse(node.textValue(), DATE_TIME_FORMAT);
	}

	private static DateTime toDateTime(LocalDate date, DateTimeZone zone) {
		return date.toDateTime(new LocalTime(12, 0), zone);
	}

	private static LocalDate getLocalDate(JsonNode node) {
		Preconditions.checkArgument(!node.isMissingNode());
		return LocalDate.parse(node.textValue(), DATE_FORMAT);
	}

	private static BigDecimal getBigDecimal(JsonNode node) {
		return BigDecimal.valueOf(node.longValue());
	}
}
