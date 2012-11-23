package com.zenobase.tasks;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Lists;

import com.zenobase.models.Event;

class FitbitStepsNode {

	private final ObjectNode node;
	private final Unit<Length> distanceUnit, heightUnit;

	public FitbitStepsNode(ObjectNode node, Unit<Length> distanceUnit, Unit<Length> heightUnit) {
		this.node = node;
		this.distanceUnit = distanceUnit;
		this.heightUnit = heightUnit;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		Event event = new Event();
		for (JsonNode distance : node.path("summary").path("distances")) {
			if ("total".equals(distance.path("activity").getTextValue())) {
				event.addValue(Event.DISTANCE, DecimalMeasure.valueOf(distance.path("distance").getDecimalValue(), distanceUnit));
			}
		}
		event.addValue(Event.TAG, "steps");
		event.addValue(Event.COUNT, node.path("summary").path("steps").getIntValue());
		event.addValue(Event.HEIGHT, DecimalMeasure.valueOf(node.path("summary").path("elevation").getDecimalValue(), heightUnit));
		// event.addValue(Event.ENERGY, Measures.valueOf(result.path("summary").path("activityCalories").getDecimalValue(), "cal"));
		events.add(event);
		return events;
	}
}
