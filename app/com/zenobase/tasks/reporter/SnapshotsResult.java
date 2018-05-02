package com.zenobase.tasks.reporter;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.elasticsearch.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import play.Logger;

import com.zenobase.common.Generator;
import com.zenobase.common.Measures;
import com.zenobase.common.Units;
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
				event.addValue(Event.TAG, textValue(optionNode));
				add |= true;
			}
			for (JsonNode tokenNode : node.path("tokens")) {
				String text = textValue(tokenNode);
				if (text != null) {
					if (q.getField() == null || Event.NOTE.getName().equals(q.getField())) {
						event.addValue(Event.NOTE, text);
						add |= true;
					}
					if (Event.TAG.getName().equals(q.getField())) {
						event.addValue(Event.TAG, text);
						add |= true;
					}
				} else {
					Logger.warn("Couldn't extract text from token node: {}", tokenNode);
				}
			}
			for (JsonNode textNode : node.path("textResponses")) {
				event.addValue(Event.NOTE, textValue(textNode));
				add |= true;
			}
			JsonNode textNode = node.path("textResponse");
			if (textNode.isTextual()) {
				event.addValue(Event.NOTE, textValue(textNode));
				add |= true;
			}
			add |= setNumericValue(node.path("numericResponse"), q, event);
			if (add) {
				events.add(event);
			}
		}
	}

	private String textValue(JsonNode tokenNode) {
		if (tokenNode.isObject()) {
			tokenNode = tokenNode.path("text");
		}
		return Strings.emptyToNull(tokenNode.textValue());
	}

	private DateTime dateTimeValue(JsonNode node) {
		int seconds = node.intValue();
		if (seconds != 0) {
			return new DateTime(2001, 1, 1, 0, 0, config.getTimezone()).plusSeconds(seconds);
		}
		String date = node.textValue();
		Preconditions.checkNotNull(date, "missing date");
		return DateTime.parse(date, ISODateTimeFormat.dateTimeNoMillis().withOffsetParsed());
	}

	private Location locationValue(JsonNode node) {
		JsonNode lat = node.path("latitude");
		JsonNode lon = node.path("longitude");
		return !lat.isMissingNode() && !lon.isMissingNode()
			? new Location(lat.decimalValue(), lon.decimalValue()) : null;
	}

	private DecimalMeasure<Temperature> temperatureValue(JsonNode node) {
		BigDecimal value = node.isNumber() ? node.decimalValue() : null;
		return value != null ? Measures.valueOf(value, Units.C) : null;
	}

	private DecimalMeasure<Pressure> pressureValue(JsonNode node) {
		BigDecimal value = node.isNumber() ? node.decimalValue() : null;
		return value != null ? Measures.valueOf(value, Units.HPA) : null;
	}

	private DecimalMeasure<Dimensionless> soundValue(JsonNode node) {
		BigDecimal value = node.isNumber() ? node.decimalValue() : null;
		return value != null ? Measures.valueOf(value, Units.DB) : null;
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
