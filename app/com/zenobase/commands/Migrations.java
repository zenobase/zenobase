package com.zenobase.commands;

import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Migrations {

	public static void replaceTimelineHistogramWithPolar(Iterable<ObjectNode> widgets) {
		for (ObjectNode widget : widgets) {
			if ("time_histogram".equals(widget.path("type"))) {
				widget.put("type", "polar");
				Logger.info("Migrated a polar widget: " + widget.path("id"));
			}
		}
	}

}
