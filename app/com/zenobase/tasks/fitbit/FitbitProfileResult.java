package com.zenobase.tasks.fitbit;

import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.JsonNode;

class FitbitProfileResult {

	private final JsonNode node;

	public FitbitProfileResult(JsonNode node) {
		this.node = node;
	}

	public DateTimeZone getTimezone() {
		String value = node.path("user").path("timezone").textValue();
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	public String getDistanceLocale() {
		return node.path("user").path("distanceUnit").textValue();
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
