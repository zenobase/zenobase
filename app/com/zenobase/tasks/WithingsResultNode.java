package com.zenobase.tasks;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Mass;
import javax.measure.unit.SI;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;

class WithingsResultNode {

	private final ObjectNode node;

	public WithingsResultNode(ObjectNode node) {
		this.node = node;
		Preconditions.checkState(node.get("status").getIntValue() == 0);
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode group : node.path("body").path("measuregrps")) {
			addEvents((ObjectNode) group, events);
		}
		return events;
	}

	private static void addEvents(ObjectNode node, List<Event> events) {
		for (JsonNode measure : node.path("measures")) {
			switch (measure.path("type").getIntValue()) {
				case 1: // weight
					Event event = new Event();
					event.addValue(Event.TAG, "body");
					event.addValue(Event.TIMESTAMP, new DateTime(node.path("date").getLongValue() * 1000, DateTimeZone.UTC));
					event.addValue(Event.WEIGHT, new DecimalMeasure<Mass>(getBigDecimal((ObjectNode) measure), SI.KILOGRAM));
					events.add(event);
			}
		}
	}

	private static BigDecimal getBigDecimal(ObjectNode node) {
		int value = node.path("value").getIntValue();
		int scale = node.path("unit").getIntValue();
		return BigDecimal.valueOf(value, -scale);
	}

	public String getMarker() {
		return node.path("body").path("updatetime").asText();
	}
}
