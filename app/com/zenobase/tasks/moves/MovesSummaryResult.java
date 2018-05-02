package com.zenobase.tasks.moves;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class MovesSummaryResult {

	static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("yyyyMMdd");
	static final Resource SOURCE = new Resource("Moves", "http://www.moves-app.com/");

	private final JsonNode node;
	private final Identity author;
	private final DateTimeZone zone;
	private final String tag;
	private final Unit<Length> lengthUnit;

	public MovesSummaryResult(JsonNode node, Identity author, DateTimeZone zone, String tag, Unit<Length> lengthUnit) {
		this.author = Preconditions.checkNotNull(author);
		this.zone = Preconditions.checkNotNull(zone);
		this.tag = Preconditions.checkNotNull(tag);
		this.lengthUnit = Preconditions.checkNotNull(lengthUnit);
		this.node = Preconditions.checkNotNull(node);
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode dayNode : node) {
			Event event = getEvent(dayNode);
			if (event != null) {
				events.add(event);
			}
		}
		return events;
	}

	public Event getEvent(JsonNode dayNode) {
		Event event = new Event();
		DateTime begin = LocalDate.parse(dayNode.path("date").textValue(), DATE_FORMAT).toDateTimeAtStartOfDay(zone);
		event.setValue(Event.TIMESTAMP, begin);
		int count = 0;
		int distance = 0;
		int energy = 0;
		for (JsonNode summaryNode : dayNode.path("summary")) {
			if (summaryNode.has("steps")) {
				count += summaryNode.path("steps").intValue();
				distance += summaryNode.path("distance").intValue();
				energy += summaryNode.path("calories").intValue();
			}
		}
		if (distance == 0) {
			return null;
		}
		event.addValue(Event.TAG, tag);
		event.setValue(Event.COUNT, count);
		if (distance > 0) {
			event.setValue(Event.DISTANCE, Measures.valueOf(Measures.convert(distance, lengthUnit), lengthUnit));
		}
		if (energy > 0) {
			event.setValue(Event.ENERGY, Measures.<Energy>valueOf(new BigDecimal(energy), Units.KCAL));
		}
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
