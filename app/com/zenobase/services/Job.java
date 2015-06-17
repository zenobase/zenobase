package com.zenobase.services;

import org.joda.time.Period;
import com.fasterxml.jackson.databind.JsonNode;

import com.zenobase.json.Nodes;

public abstract class Job {

	private String label;
	private Period period;

	public Job(String label, Period period) {
		this.label = label;
		this.period = period;
	}

	public Period getPeriod() {
		return period;
	}

	public abstract void run();

	public JsonNode toJson() {
		return Nodes.newObject()
			.put("label", label)
			.put("period", period.toString());
	}
}
