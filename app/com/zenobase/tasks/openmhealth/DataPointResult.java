package com.zenobase.tasks.openmhealth;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Quantity;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.VolumetricDensity;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import play.Logger;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Resource;

public class DataPointResult {

	public static final Resource SOURCE = new Resource("Open mHealth", "http://www.openmhealth.org/");

	private final Identity author;
	private final JsonNode node;

	public DataPointResult(Identity author, JsonNode node) {
		this.author = author;
		this.node = node;
	}

	public Event getEvent() {
		String namespace = node.path("header").path("schema_id").path("namespace").textValue();
		if (!"omh".equals(namespace)) {
			return null;
		}
		Event event = new Event();
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		addTag(node.path("header").path("schema_id").path("name"), event);
		if (!event.contains(Event.TAG)) {
			Logger.warn("Can't find a schema name: {}", node.path("header"));
			return null;
		}
		addTimestamp(node.path("body").path("effective_time_frame"), event);
		if (!event.contains(Event.TIMESTAMP)) {
			Logger.warn("Can't find a timestamp: {}", node.path("body").path("effective_time_frame"));
			return null;
		}
		addFields(node.path("body"), event);
		return event;
	}

	private static void addFields(JsonNode node, Event event) {
		addConcentration(node.path("blood_glucose"), event);
		addPressure(node.path("systolic_blood_pressure"), event);
		addPressure(node.path("diastolic_blood_pressure"), event);
		addTemperature(node.path("body_temperature"), event);
		addPercentage(node.path("body_fat_percentage"), event);
		addHeight(node.path("body_height"), event);
		addWeight(node.path("body_weight"), event);
		addEnergy(node.path("kcal_burned"), event);
		addTag(node.path("measurement_location"), event);
		addTag(node.path("activity_name"), event);
		addTag(node.path("temporal_relationship_to_physical_activity"), event);
		addNote(node.path("user_notes"), event);
		addFrequency(node.path("heart_rate"), event);
		addDistance(node.path("distance"), event);
		addCount(node.path("step_count"), event);
	}

	private static void addTimestamp(JsonNode node, Event event) {
		DateTime begin = dateTimeValue(node.path("time_interval").path("start_date_time"));
		DateTime end = dateTimeValue(node.path("time_interval").path("end_date_time"));
		if (begin != null && end != null) {
			event.addValue(Event.TIMESTAMP, begin);
			event.addValue(Event.TIMESTAMP, end);
			event.setValue(Event.DURATION, new Duration(begin, end));
		} else {
			event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("date_time")));
		}
	}

	private static void addConcentration(JsonNode node, Event event) {
		event.addValue(Event.CONCENTRATION, measureValue(node, VolumetricDensity.class));
	}

	private static void addPressure(JsonNode node, Event event) {
		event.addValue(Event.PRESSURE, measureValue(node, Pressure.class));
	}

	private static void addTemperature(JsonNode node, Event event) {
		event.addValue(Event.TEMPERATURE, measureValue(node, Temperature.class));
	}

	private static void addPercentage(JsonNode node, Event event) {
		event.addValue(Event.PERCENTAGE, percentageValue(node));
	}

	private static void addHeight(JsonNode node, Event event) {
		event.addValue(Event.HEIGHT, measureValue(node, Length.class));
	}

	private static void addWeight(JsonNode node, Event event) {
		event.addValue(Event.WEIGHT, measureValue(node, Mass.class));
	}

	private static void addEnergy(JsonNode node, Event event) {
		event.addValue(Event.ENERGY, measureValue(node, Energy.class));
	}

	private static void addTag(JsonNode node, Event event) {
		event.addValue(Event.TAG, node.textValue());
	}

	private static void addNote(JsonNode node, Event event) {
		event.addValue(Event.NOTE, node.textValue());
	}

	private static void addFrequency(JsonNode node, Event event) {
		event.addValue(Event.FREQUENCY, measureValue(node, Frequency.class));
	}

	private static void addDistance(JsonNode node, Event event) {
		event.addValue(Event.DISTANCE, measureValue(node, Length.class));
	}

	private static void addCount(JsonNode node, Event event) {
		event.addValue(Event.COUNT, intValue(node));
	}

	private static DateTime dateTimeValue(JsonNode node) {
		return node.isTextual() ? DateTime.parse(node.textValue()) : null;
	}

	private static <Q extends Quantity> DecimalMeasure<Q> measureValue(JsonNode node, Class<Q> q) {
		String unit = unitValue(node.path("unit"));
		BigDecimal value = node.path("value").decimalValue();
		return unit != null ? Measures.<Q>valueOf(Measures.round(value), unit) : null;
	}

	private static Percentage percentageValue(JsonNode node) {
		String unit = unitValue(node.path("unit"));
		BigDecimal value = node.path("value").decimalValue();
		return "%".equals(unit) ? Percentage.valueOf(value) : null;
	}

	private static String unitValue(JsonNode node) {
		return node.isTextual() ? node.textValue().replace(' ', '_').replace("beats/min", "bpm") : null;
	}

	private static Integer intValue(JsonNode node) {
		return node.isInt() ? node.intValue() : null;
	}
}
