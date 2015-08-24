package com.zenobase.tasks.beeminder;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;

import com.zenobase.json.Nodes;

class Datapoint {

	private final DateTime time;
	private final BigDecimal value;

	public Datapoint(DateTime time, BigDecimal value) {
		this.time = time;
		this.value = value;
	}

	public ObjectNode toJson(boolean asDuration) {
		ObjectNode node = Nodes.newObject()
			.put("timestamp", time.getMillis() / 1000)
			.put("comment", "");
		if (asDuration) {
			node.put("value", formatDuration(value));
		} else {
			node.put("value", value);
		}
		return node;
	}

	static String formatDuration(BigDecimal value) {
		long t = value.longValueExact() / 1000;
		long s = t % 60;
		t /= 60;
		long m = t % 60;
		t /= 60;
		return String.format("%d:%02d:%02d", t, m, s);
	}
}
