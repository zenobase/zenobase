package com.zenobase.tasks.runkeeper;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.JsonNode;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;

class RunkeeperWeightResult extends RunkeeperResultSupport {

	private final String tag;
	private final Unit<Mass> unit;

	public RunkeeperWeightResult(JsonNode node, Identity author, String tag, Unit<Mass> unit, DateTimeZone timezone) {
		super(node, author, timezone);
		this.tag = tag;
		this.unit = unit;
	}

	@Override
	protected Event newEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("timestamp")));
		event.setValue(Event.WEIGHT, massValue(node.path("weight")));
		event.setValue(Event.PERCENTAGE, percentageValue(node.path("fat_percent")));
		event.setValue(Event.SOURCE, resourceValue(node.path("uri")));
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	private DecimalMeasure<Mass> massValue(JsonNode node) {
		return !isZero(node) ? Measures.valueOf(Measures.convert(node.doubleValue(), unit), unit) : null;
	}

	private Percentage percentageValue(JsonNode node) {
		return !isZero(node) ? Percentage.valueOf(node.decimalValue()) : null;
	}
}
