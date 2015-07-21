package com.zenobase.tasks.microsoft;

import java.math.MathContext;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.Duration;

class ActivitiesResult {

	static final Resource SOURCE = new Resource("Microsoft Health", "https://www.microsoft.com/microsoft-health/");

	private final JsonNode node;
	private final Identity author;
	private final boolean metric;

	public ActivitiesResult(JsonNode node, Identity author, boolean metric) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.metric = metric;
		System.err.println(node);
	}

	public String next() {
		return node.path("nextPage").textValue();
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode tripNode : node.path("results")) {
			events.add(newEvent(tripNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("startTime")));
		event.setValue(Event.DURATION, durationValue(node.path("duration")));
		event.addValue(Event.TAG, node.path("activityType").textValue());
		for (JsonNode mapPoint : node.path("mapPoints")) {
			Location location = locationValue(mapPoint.path("location"));
			if (location != null) {
				event.setValue(Event.LOCATION, location);
				break;
			}
		}
		event.setValue(Event.DISTANCE, Measures.round(distanceValue(node.path("distanceSummary").path("totalDistance"))));
		event.setValue(Event.HEIGHT, Measures.round(heightValue(node.path("distanceSummary").path("altitudeGain"))));
		event.setValue(Event.FREQUENCY, Measures.round(frequencyValue(node.path("heartRateSummary").path("averageHeartRate"))));
		event.setValue(Event.ENERGY, Measures.round(energyValue(node.path("caloriesBurnedSummary").path("totalCalories"))));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	private static DateTime dateTimeValue(JsonNode node) {
		return DateTime.parse(node.textValue());
	}

	private Duration durationValue(JsonNode node) {
		return !node.isNull() ? Duration.parse(node.textValue()) : null;
	}

	private static Location locationValue(JsonNode node) {
		if (node.isMissingNode() || node.isNull()) {
			return null;
		}
		Preconditions.checkState(node.path("latitude").isNumber(), "expected a numeric latitude in <%s>", node);
		Preconditions.checkState(node.path("longitude").isNumber(), "expected a numeric longitude in <%s>", node);
		return new Location(node.path("latitude").decimalValue(), node.path("longitude").decimalValue());
	}

	private DecimalMeasure<Length> distanceValue(JsonNode node) {
		if (!node.isNumber()) {
			return null;
		}
		DecimalMeasure<Length> value = Measures.valueOf(node.decimalValue(), Units.M);
		return value.to(metric ? Units.KM : Units.MI, MathContext.DECIMAL32);
	}

	private DecimalMeasure<Length> heightValue(JsonNode node) {
		if (!node.isNumber()) {
			return null;
		}
		DecimalMeasure<Length> value = Measures.valueOf(node.decimalValue(), Units.M);
		return value.to(metric ? Units.M : Units.FT, MathContext.DECIMAL32);
	}

	private DecimalMeasure<Frequency> frequencyValue(JsonNode node) {
		return node.isNumber() ? Measures.valueOf(node.decimalValue(), Units.BPM) : null;
	}

	private DecimalMeasure<Energy> energyValue(JsonNode node) {
		return node.isNumber() ? Measures.valueOf(node.decimalValue(), Units.KCAL) : null;
	}
}
