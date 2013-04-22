package com.zenobase.migrate;

import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import com.google.common.collect.Lists;

import com.zenobase.models.Bucket;

public class Migrate21to22 {

	public static Bucket migrate(Bucket bucket) {
		List<ObjectNode> widgets = Lists.newArrayList();
		boolean touched = false;
		for (ObjectNode widget : bucket.getWidgets()) {
			widgets.add(widget);
			if ("plot".equals(widget.path("type").getTextValue())) {
				widget.put("type", "timeline");
				touched = true;
			} else if ("correlate".equals(widget.path("type").getTextValue())) {
				widget.put("type", "scatterplot");
				touched = true;
			}
		}
		if (touched) {
			bucket.setWidgets(widgets);
		}
		return bucket;
	}
}
