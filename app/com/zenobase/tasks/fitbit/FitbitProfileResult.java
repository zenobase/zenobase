package com.zenobase.tasks.fitbit;

import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.codehaus.jackson.JsonNode;
import org.joda.time.DateTimeZone;

class FitbitProfileResult {

	private final JsonNode node;

	public FitbitProfileResult(JsonNode node) {
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
