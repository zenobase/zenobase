package com.zenobase.tasks.ihealth;

import java.math.RoundingMode;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTimeZone;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class IHealthFoodResult extends IHealthResultSupport {

	private final String tag;
	private final DateTimeZone zone;

	public IHealthFoodResult(JsonNode node, Identity author, String tag, DateTimeZone zone) {
		super("FoodDataList", node, author);
		this.tag = Preconditions.checkNotNull(tag);
		this.zone = Preconditions.checkNotNull(zone);
	}

	@Override
	protected Event newEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		String kind = textValue(node.path("FoodKind"));
		if (kind != null) {
			event.addValue(Event.TAG, kind);
		}
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("MDate"), zone));
		event.setValue(Event.ENERGY, energyValue(node.path("Calories")));
		event.setValue(Event.WEIGHT, weightValue(node.path("Amount")));
		event.setValue(Event.LOCATION, locationValue(node));
		event.setValue(Event.NOTE, textValue(node.path("Note")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private DecimalMeasure<Mass> weightValue(JsonNode node) {
		return !isZero(node) ? Measures.valueOf(node.decimalValue().setScale(0, RoundingMode.HALF_UP), getUnit()) : null;
	}

	private Unit<Mass> getUnit() {
		int unit = node.path("FoodUnit").intValue();
		switch (unit) {
			case 0: return Units.OZ;
			case 1: return Units.G;
			default: throw new IllegalArgumentException("Can't handle unit: " + unit);
		}
	}
}
