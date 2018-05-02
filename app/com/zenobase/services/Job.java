package com.zenobase.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.LocalTime;
import org.joda.time.Period;

import com.zenobase.json.Nodes;

public abstract class Job {

	private String label;
	private LocalTime begin;
	private Period period;

	public Job(String label, LocalTime begin, Period period) {
		this.label = label;
		this.begin = begin;
		this.period = period;
	}

	public LocalTime getBegin() {
		return begin;
	}

	public Period getPeriod() {
		return period;
	}

	public abstract void run();

	public JsonNode toJson() {
		return Nodes.newObject()
			.put("label", label)
			.put("begin", begin.toString("HH:mm"))
			.put("period", period.toString());
	}
}
