package com.zenobase.tasks;

import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTimeZone;

class FitbitProfileNode {

	private final ObjectNode node;

	public FitbitProfileNode(ObjectNode node) {
		this.node = node;
	}

	public DateTimeZone getTimezone() {
		return DateTimeZone.forOffsetMillis(node.path("user").path("offsetFromUTCMillis").getIntValue());
	}

	public String getDistanceLocale() {
		return node.path("user").path("distanceUnit").getTextValue();
	}

	public Unit<Length> getDistanceUnit() {
		return "en_US".equals(getDistanceLocale()) ?
			NonSI.MILE : SI.KILOMETER;
	}

	public Unit<Length> getHeightUnit() {
		return "en_US".equals(getDistanceLocale()) ?
			NonSI.FOOT : SI.METER;
	}
}
