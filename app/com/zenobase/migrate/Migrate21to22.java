package com.zenobase.migrate;

import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.models.Bucket;

public class Migrate21to22 {

	public static void migrate(Bucket bucket) {
		for (ObjectNode widget : bucket.getWidgets()) {
			if ("plot".equals(widget.get("type"))) {
				widget.put("type", "timeline");
			} else if ("correlate".equals(widget.get("type"))) {
				widget.put("type", "scatterplot");
			}
		}
	}
}
