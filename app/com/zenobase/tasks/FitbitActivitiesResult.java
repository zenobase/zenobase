package com.zenobase.tasks;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import org.codehaus.jackson.JsonNode;
import org.joda.time.DateTime;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class FitbitActivitiesResult {


	public static final Resource SOURCE = new Resource("Fitbit", "http://fitbit.com/");

	private final JsonNode node;
	private final String tag;
	private final Identity author;
	private final DateTime timestamp;
	private final Unit<Length> distanceUnit, heightUnit;

	public FitbitActivitiesResult(JsonNode node, String tag, Identity author, DateTime timestamp, Unit<Length> distanceUnit, Unit<Length> heightUnit) {
		this.node = node;
		this.tag = tag;
		this.author = author;
		this.timestamp = timestamp;
		this.distanceUnit = distanceUnit;
		this.heightUnit = heightUnit;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		int steps = getSteps();
		if (steps > 0) {
			Event event = new Event();
			event.setValue(Event.TAG, tag);
			event.setValue(Event.COUNT, steps);
			event.setValue(Event.DISTANCE, getDistance());
			event.setValue(Event.HEIGHT, getElevation());
			event.setValue(Event.ENERGY, getCalories());
			event.setValue(Event.TIMESTAMP, timestamp);
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
			events.add(event);
		}
		return events;
	}

	private int getSteps() {
		return node.path("summary").path(tag).getIntValue();
	}

	private DecimalMeasure<Length> getDistance() {
		for (JsonNode distance : node.path("summary").path("distances")) {
			if ("total".equals(distance.path("activity").getTextValue())) {
				BigDecimal value = distance.path("distance").getDecimalValue();
				return value != null ? DecimalMeasure.valueOf(value, distanceUnit) : null;
			}
		}
		return null;
	}

	private DecimalMeasure<Length> getElevation() {
		BigDecimal value = node.path("summary").path("elevation").getDecimalValue();
		return value != null ? DecimalMeasure.valueOf(value, heightUnit) : null;
	}

	private DecimalMeasure<Energy> getCalories() {
		BigDecimal value = node.path("summary").path("activityCalories").getDecimalValue();
		Unit<Energy> unit = Measures.parseUnit("cal");
		return value != null ? DecimalMeasure.valueOf(value, unit) : null;
	}
}
