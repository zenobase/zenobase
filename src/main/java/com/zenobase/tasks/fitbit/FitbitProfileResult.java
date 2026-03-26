package com.zenobase.tasks.fitbit;

import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTimeZone;

import com.zenobase.common.Units;

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
		return "en_US".equals(getDistanceLocale()) ? Units.MI : Units.KM;
	}

	public Unit<Length> getHeightUnit() {
		return "en_US".equals(getDistanceLocale()) ? Units.FT : Units.M;
	}

	public String getWeightLocale() {
		return node.path("user").path("weightUnit").textValue();
	}

	public Unit<Mass> getWeightUnit() {
		String locale = getWeightLocale();
		if ("en_US".equals(locale)) {
			return Units.LB;
		}
		if ("en_GB".equals(locale)) {
			return Units.ST;
		}
		return Units.KG;
	}
}
