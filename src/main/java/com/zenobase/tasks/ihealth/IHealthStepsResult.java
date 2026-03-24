package com.zenobase.tasks.ihealth;

import java.math.RoundingMode;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTimeZone;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class IHealthStepsResult extends IHealthResultSupport {

	private final String tag;
	private final DateTimeZone zone;

	public IHealthStepsResult(JsonNode node, Identity author, String tag, DateTimeZone zone) {
		super("ARDataList", node, author);
		this.tag = Preconditions.checkNotNull(tag);
		this.zone = Preconditions.checkNotNull(zone);
	}

	@Override
	protected Event newEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("MDate"), zone));
		event.setValue(Event.COUNT, intValue(node.path("Steps")));
		event.setValue(Event.ENERGY, energyValue(node.path("Calories")));
		event.setValue(Event.DISTANCE, distanceValue(node.path("DistanceTraveled")));
		event.setValue(Event.LOCATION, locationValue(node));
		event.setValue(Event.NOTE, textValue(node.path("Note")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private Integer intValue(JsonNode node) {
		return !isZero(node) ? node.intValue() : null;
	}

	private DecimalMeasure<Length> distanceValue(JsonNode node) {
		return !isZero(node) ? Measures.valueOf(node.decimalValue().setScale(2, RoundingMode.HALF_UP), getUnit()) : null;
	}

	private Unit<Length> getUnit() {
		int unit = node.path("DistanceUnit").intValue();
		switch (unit) {
			case 0: return Units.KM;
			case 1: return Units.MI;
			default: throw new IllegalArgumentException("Can't handle unit: " + unit);
		}
	}
}
