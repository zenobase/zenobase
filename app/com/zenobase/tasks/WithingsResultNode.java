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
import com.zenobase.models.Identity;

class WithingsResultNode {

	private final Identity author;
	private final ObjectNode node;

	public WithingsResultNode(Identity author, ObjectNode node) {
		this.author = author;
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

	private void addEvents(ObjectNode node, List<Event> events) {
		for (JsonNode measure : node.path("measures")) {
			switch (measure.path("type").getIntValue()) {
				case 1: // weight
					Event event = new Event();
					event.addValue(Event.TAG, "body");
					event.addValue(Event.WEIGHT, new DecimalMeasure<Mass>(getBigDecimal((ObjectNode) measure), SI.KILOGRAM));
					event.addValue(Event.TIMESTAMP, new DateTime(node.path("date").getLongValue() * 1000, DateTimeZone.UTC));
					event.setValue(Event.AUTHOR, author);
					events.add(event);
			}
		}
	}

	private static BigDecimal getBigDecimal(ObjectNode node) {
		int value = node.path("value").getIntValue();
		int scale = node.path("unit").getIntValue();
		return BigDecimal.valueOf(value, -scale);
	}

	public Long getMarker() {
		return node.path("body").path("updatetime").getLongValue();
	}
}
