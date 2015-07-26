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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

class StepsResult extends MicrosoftHealthResultSupport {

	private final String tag;
	private final boolean metric;

	public StepsResult(JsonNode node, Identity author, DateTimeZone zone, String tag, boolean metric) {
		super(node, author, zone);
		this.tag = tag;
		this.metric = metric;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode summaryNode : node.path("summaries")) {
			events.add(newEvent(summaryNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		Event event = new Event();
		DateTime begin = dateTimeValue(node.path("startTime"));
		DateTime end = dateTimeValue(node.path("endTime"));
		event.setValue(Event.TIMESTAMP, begin);
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.addValue(Event.TAG, tag);
		event.setValue(Event.DISTANCE, distanceValue(node.path("distanceSummary").path("totalDistance")));
		event.setValue(Event.HEIGHT, heightValue(node.path("distanceSummary").path("elevationGain")));
		event.setValue(Event.FREQUENCY, frequencyValue(node.path("heartRateSummary").path("averageHeartRate")));
		event.setValue(Event.ENERGY, energyValue(node.path("caloriesBurnedSummary").path("totalCalories")));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	private DecimalMeasure<Length> distanceValue(JsonNode node) {
		if (!node.isNumber()) {
			return null;
		}
		DecimalMeasure<Length> value = Measures.valueOf(node.decimalValue(), Units.CM);
		return Measures.round(value.to(metric ? Units.KM : Units.MI, MathContext.DECIMAL32));
	}

	private DecimalMeasure<Length> heightValue(JsonNode node) {
		if (!node.isNumber()) {
			return null;
		}
		DecimalMeasure<Length> value = Measures.valueOf(node.decimalValue(), Units.CM);
		return Measures.round(value.to(metric ? Units.M : Units.FT, MathContext.DECIMAL32), 0);
	}

	private DecimalMeasure<Frequency> frequencyValue(JsonNode node) {
		return node.isNumber() ? Measures.valueOf(Measures.round(node.decimalValue(), 0), Units.BPM) : null;
	}

	private DecimalMeasure<Energy> energyValue(JsonNode node) {
		return node.isNumber() ? Measures.valueOf(Measures.round(node.decimalValue(), 0), Units.KCAL) : null;
	}
}
