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

class IHealthWeightResult extends IHealthResultSupport {

	private final String tag;
	private final DateTimeZone zone;

	public IHealthWeightResult(JsonNode node, Identity author, String tag, DateTimeZone zone) {
		super("WeightDataList", node, author);
		this.tag = Preconditions.checkNotNull(tag);
		this.zone = Preconditions.checkNotNull(zone);
	}

	@Override
	protected Event newEvent(JsonNode node) {
		var event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("MDate"), zone));
		event.setValue(Event.WEIGHT, weightValue(node.path("WeightValue")));
		event.setValue(Event.PERCENTAGE, percentageValue(node.path("FatValue")));
		event.setValue(Event.NOTE, textValue(node.path("Note")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private DecimalMeasure<Mass> weightValue(JsonNode node) {
		return !isZero(node) ? Measures.valueOf(node.decimalValue().setScale(2, RoundingMode.HALF_UP), getUnit()) : null;
	}

	private Unit<Mass> getUnit() {
		int unit = node.path("WeightUnit").intValue();
		switch (unit) {
			case 0: return Units.KG;
			case 1: return Units.LB;
			case 2: return Units.ST;
			default: throw new IllegalArgumentException("Can't handle unit: " + unit);
		}
	}
}
