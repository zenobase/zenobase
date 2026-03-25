package com.zenobase.tasks.ihealth;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.VolumetricDensity;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTimeZone;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class IHealthGlucoseResult extends IHealthResultSupport {

	private static final BigDecimal MMOL_PER_L_TO_MG_PER_DL = new BigDecimal("18.0182");

	private final String tag;
	private final DateTimeZone zone;

	public IHealthGlucoseResult(JsonNode node, Identity author, String tag, DateTimeZone zone) {
		super("BGDataList", node, author);
		this.tag = Preconditions.checkNotNull(tag);
		this.zone = Preconditions.checkNotNull(zone);
	}

	@Override
	protected Event newEvent(JsonNode node) {
		var event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("MDate"), zone));
		event.setValue(Event.CONCENTRATION, concentrationValue(node.path("BG")));
		event.setValue(Event.LOCATION, locationValue(node));
		event.setValue(Event.NOTE, textValue(node.path("Note")));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private DecimalMeasure<VolumetricDensity> concentrationValue(JsonNode node) {
		BigDecimal value = node.decimalValue();
		if (BigDecimal.ZERO.equals(value)) {
			return null;
		}
		value = value.multiply(getConversionFactor()).setScale(0, RoundingMode.HALF_UP);
		return Measures.valueOf(value, Units.MG_PER_DL);
	}

	private BigDecimal getConversionFactor() {
		int unit = node.path("BGUnit").intValue();
		switch (unit) {
			case 0: return BigDecimal.ONE;
			case 1: return MMOL_PER_L_TO_MG_PER_DL;
			default: throw new IllegalArgumentException("Can't handle unit: " + unit);
		}
	}
}
