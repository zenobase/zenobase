package com.zenobase.tasks.runkeeper;

import java.util.Objects;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;

class RunkeeperActivityResult {

	private final JsonNode node;
	private final Unit<Length> unit;

	public RunkeeperActivityResult(JsonNode node, Unit<Length> unit) {
		this.node = Preconditions.checkNotNull(node);
		this.unit = unit;
	}

	public void addDetails(Event event) {
		event.setValue(Event.NOTE, node.path("notes").textValue());
		event.setValue(Event.HEIGHT, convertMeasureValue(node.path("climb"), unit));
		event.setValue(Event.FREQUENCY, measureValue(node.path("average_heart_rate"), Units.BPM));
		event.setValue(
				Event.SOURCE, new Resource("RunKeeper", node.path("activity").textValue()));
		event.setValue(Event.LOCATION, locationValue(node.path("path")));
	}

	private static @Nullable <Q extends Quantity> DecimalMeasure<Q> measureValue(JsonNode node, Unit<Q> unit) {
		return !isZero(node) ? Measures.valueOf(node.decimalValue(), unit) : null;
	}

	private static @Nullable <Q extends Quantity> DecimalMeasure<Q> convertMeasureValue(JsonNode node, Unit<Q> unit) {
		return !isZero(node)
				? Measures.valueOf(
						Objects.requireNonNull(Measures.round(Measures.convert(node.doubleValue(), unit), 0)), unit)
				: null;
	}

	private static @Nullable Location locationValue(JsonNode path) {
		for (JsonNode node : path) {
			if ("start".equals(node.path("type").textValue())) {
				return new Location(
						node.path("latitude").decimalValue(),
						node.path("longitude").decimalValue());
			}
		}
		return null;
	}

	private static boolean isZero(JsonNode node) {
		Preconditions.checkArgument(node.isMissingNode() || node.isNull() || node.isNumber());
		return node.doubleValue() == 0.0;
	}
}
