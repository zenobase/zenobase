package com.zenobase.commands;

import play.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Event;

public class Migrations {

	public static void replaceTimelineHistogramWithPolar(Iterable<ObjectNode> widgets) {
		for (ObjectNode widget : widgets) {
			if ("time_histogram".equals(widget.path("type").textValue())) {
				Logger.info("Migrating a polar widget...");
				widget.put("type", "polar");
			}
		}
	}

	public static void rewriteDuration(Event event) {
		JsonNode node = event.toJson().path(Event.DURATION.getName());
		if (node.isArray()) {
			boolean fix = false;
			for (JsonNode value : node) {
				if (value.isTextual()) {
					fix = true;
					break;
				}
			}
			if (fix) {
				Logger.info("Converting one or more durations to numeric values...");
				event.setValues(Event.DURATION, event.getValues(Event.DURATION));
			}
		} else  if (node.isTextual()) {
			Logger.info("Converting duration to numeric value...");
			event.setValue(Event.DURATION, event.getValue(Event.DURATION));
		}
	}
}
