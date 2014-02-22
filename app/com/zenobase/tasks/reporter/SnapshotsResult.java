package com.zenobase.tasks.reporter;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.joda.time.DateTime;
import play.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.common.Generator;
import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;

public class SnapshotsResult {

	public static final Resource SOURCE = new Resource("Reporter", "http://www.reporter-app.com/");

	private final Configuration config;
	private final Identity author;
	private final JsonNode node;

	public SnapshotsResult(Configuration config, Identity author, JsonNode node) {
		this.config = config;
		this.author = author;
		this.node = node;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode snapshotNode : node.path("snapshots")) {
			addSnapshot(snapshotNode, events);
		}
		return events;
	}

	private void addSnapshot(JsonNode node, List<Event> events) {
		Event event = new Event();
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("date")));
		event.setValue(Event.LOCATION, locationValue(node.path("location")));
		event.setValue(Event.TEMPERATURE, temperatureValue(node.path("weather").path("tempC")));
		event.setValue(Event.PRESSURE, pressureValue(node.path("weather").path("pressureMb")));
		event.setValue(Event.SOUND, soundValue(node.path("audio").path("avg")));
		for (JsonNode responseNode : node.path("responses")) {
			addResponse(responseNode, event.copy(), events);
		}
	}

	private void addResponse(JsonNode node, Event event, List<Event> events) {
		String prompt = node.path("questionPrompt").textValue();
		Question q = config.getQuestion(prompt);
		if (q != null) {
			event.setValue(Event.ID, Generator.id());
			boolean add = false;
			if (q.getTag() != null) {
				event.addValue(Event.TAG, q.getTag());
			}
			for (JsonNode optionNode : node.path("answeredOptions")) {
				event.addValue(Event.TAG, optionNode.textValue());
				add |= true;
			}
			for (JsonNode tokenNode : node.path("tokens")) {
				event.addValue(Event.NOTE, tokenNode.textValue());
				add |= true;
			}
			add |= setNumericValue(node.path("numericResponse"), q, event);
			if (add) {
				events.add(event);
			} else {
				Logger.warn("Could't extract any values from: " + node);
			}
		}
	}

	private DateTime dateTimeValue(JsonNode node) {
		int seconds = node.intValue();
		Preconditions.checkArgument(seconds != 0, "missing date");
		return new DateTime(2001, 1, 1, 0, 0, config.getTimezone()).plusSeconds(seconds);
	}

	private Location locationValue(JsonNode node) {
		BigDecimal lat = node.path("latitude").decimalValue();
		BigDecimal lon = node.path("longitude").decimalValue();
		Preconditions.checkArgument(!BigDecimal.ZERO.equals(lat), "missing latitude");
		Preconditions.checkArgument(!BigDecimal.ZERO.equals(lon), "missing longitude");
		return new Location(lat, lon);
	}

	private DecimalMeasure<Temperature> temperatureValue(JsonNode node) {
		BigDecimal value = node.isNumber() ? node.decimalValue() : null;
		return value != null ? Measures.valueOf(value, SI.CELSIUS) : null;
	}

	private DecimalMeasure<Pressure> pressureValue(JsonNode node) {
		BigDecimal value = node.isNumber() ? node.decimalValue() : null;
		return value != null ? Measures.valueOf(value, SI.HECTO(SI.PASCAL)) : null;
	}

	private DecimalMeasure<Dimensionless> soundValue(JsonNode node) {
		BigDecimal value = node.isNumber() ? node.decimalValue() : null;
		return value != null ? Measures.valueOf(value, NonSI.DECIBEL) : null;
	}

	private boolean setNumericValue(JsonNode node, Question q, Event event) {
		if (node.asInt() > 0 || "0".equals(node.textValue())) {
			if (q.getField() == null || Event.COUNT.getName().equals(q.getField())) {
				event.setValue(Event.COUNT, node.asInt());
				return true;
			}
			if (Event.RATING.getName().equals(q.getField())) {
				event.setValue(Event.RATING, Rating.valueOf(node.asInt() * 10));
				return true;
			}
		}
		return false;
	}
}
