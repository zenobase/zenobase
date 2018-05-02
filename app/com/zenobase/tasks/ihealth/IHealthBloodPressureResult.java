package com.zenobase.tasks.ihealth;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Pressure;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTimeZone;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class IHealthBloodPressureResult extends IHealthResultSupport {

	private final String tag;
	private final DateTimeZone zone;

	public IHealthBloodPressureResult(JsonNode node, Identity author, String tag, DateTimeZone zone) {
		super("BPDataList", node, author);
		this.tag = Preconditions.checkNotNull(tag);
		this.zone = Preconditions.checkNotNull(zone);
	}

	@Override
	protected Event newEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("MDate"), zone));
		event.addValue(Event.PRESSURE, pressureValue(node.path("LP")));
		event.addValue(Event.PRESSURE, pressureValue(node.path("HP")));
		event.setValue(Event.FREQUENCY, frequencyValue(node.path("HR")));
		event.setValue(Event.LOCATION, locationValue(node));
		event.setValue(Event.NOTE, textValue(node.path("Note")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private DecimalMeasure<Pressure> pressureValue(JsonNode node) {
		return !isZero(node) ? Measures.valueOf(node.decimalValue(), getUnit()) : null;
	}

	private Unit<Pressure> getUnit() {
		int unit = node.path("BPUnit").intValue();
		switch (unit) {
			case 0: return Units.MMHG;
			case 1: return Units.KPA;
			default: throw new IllegalArgumentException("Can't handle unit: " + unit);
		}
	}
}
