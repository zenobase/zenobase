package com.zenobase.tasks.runkeeper;

import java.util.Objects;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;

class RunkeeperWeightResult extends RunkeeperResultSupport {

	private final @Nullable String tag;
	private final @Nullable Unit<Mass> unit;

	public RunkeeperWeightResult(
			JsonNode node, Identity author, @Nullable String tag, @Nullable Unit<Mass> unit, DateTimeZone timezone) {
		super(node, author, timezone);
		this.tag = tag;
		this.unit = unit;
	}

	@Override
	protected Event newEvent(JsonNode node) {
		var event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("timestamp")));
		event.setValue(Event.WEIGHT, massValue(node.path("weight")));
		event.setValue(Event.PERCENTAGE, percentageValue(node.path("fat_percent")));
		event.setValue(Event.SOURCE, resourceValue(node.path("uri")));
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	private @Nullable DecimalMeasure<Mass> massValue(JsonNode node) {
		return !isZero(node)
				? Measures.valueOf(
						Objects.requireNonNull(Measures.convert(node.doubleValue(), Objects.requireNonNull(unit))),
						Objects.requireNonNull(unit))
				: null;
	}

	private @Nullable Percentage percentageValue(JsonNode node) {
		return !isZero(node) ? Percentage.valueOf(node.decimalValue()) : null;
	}
}
