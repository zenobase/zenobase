package com.zenobase.tasks.runkeeper;

import java.math.MathContext;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class ActivitiesResult {

	static final Resource SOURCE = new Resource("RunKeeper", "http://runkeeper.com/");
	static final DateTimeFormatter TIME_FORMAT = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss");

	private final JsonNode node;
	private final Identity author;
	private final Unit<Length> unit;
	private final DateTimeZone timezone;

	public ActivitiesResult(JsonNode node, Identity author, Unit<Length> unit, DateTimeZone timezone) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.unit = unit;
		this.timezone = timezone;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode itemNode : node.path("items")) {
			events.add(newEvent(itemNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TAG, node.path("type").textValue());
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("start_time")));
		event.setValue(Event.DURATION, durationValue(node.path("duration")));
		event.setValue(Event.DISTANCE, distanceValue(node.path("total_distance")));
		event.setValue(Event.ENERGY, energyValue(node.path("total_calories")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private DateTime dateTimeValue(JsonNode node) {
		LocalDateTime local = TIME_FORMAT.parseLocalDateTime(node.textValue());
		Preconditions.checkState(!timezone.isLocalDateTimeGap(local), "<%s> does not exist in <%s>", local, timezone);
		return local.toDateTime(timezone);
	}

	private Duration durationValue(JsonNode node) {
		return node.isNumber() ? Duration.millis(node.decimalValue().movePointRight(3).longValue()) : null;
	}

	private DecimalMeasure<Length> distanceValue(JsonNode node) {
		return node.isNumber() ? Measures.valueOf(node.decimalValue(), SI.METER).to(unit, MathContext.DECIMAL64) : null;
	}

	private DecimalMeasure<Energy> energyValue(JsonNode node) {
		return node.isNumber() ? Measures.<Energy>valueOf(node.decimalValue(), "cal") : null;
	}

	public String getNext() {
		return node.path("next").textValue();
	}
}
