package com.zenobase.tasks.forecast;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Quantity;
import javax.measure.quantity.Temperature;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Percentage;

public class ForecastResult {

	private final ObjectNode node;
	private final boolean standardUnits;

	public ForecastResult(ObjectNode node, boolean standardUnits) {
		this.node = node;
		this.standardUnits = standardUnits;
	}

	public Forecast get(DateTime time) {
		String tag = node.path("currently").path("summary").textValue();
		DecimalMeasure<Temperature> temperature = measureValue(node.path("currently").path("temperature"), getTemperatureUnit());
		DecimalMeasure<Pressure> pressure = measureValue(node.path("currently").path("pressure"), Units.HPA);
		Integer humidity = percentValue(node.path("currently").path("humidity"));
		Percentage moon = lunationValue(node.path("daily").path("data").path(0).path("moonPhase"));
		return new Forecast(tag, temperature, pressure, humidity, moon);
	}

	private Unit<Temperature> getTemperatureUnit() {
		return standardUnits ? Units.C : Units.F;
	}

	private static <Q extends Quantity> DecimalMeasure<Q> measureValue(JsonNode node, Unit<Q> unit) {
		if (node.isMissingNode()) {
			return null;
		}
		BigDecimal value = node.decimalValue();
		return Measures.valueOf(value, unit);
	}

	private static Integer percentValue(JsonNode node) {
		if (node.isMissingNode()) {
			return null;
		}
		return node.decimalValue().movePointRight(2).intValue();
	}

	private static Percentage lunationValue(JsonNode node) {
		return !node.isMissingNode() ? moonPhaseToPercentage(node.doubleValue()) : null;
	}

	static Percentage moonPhaseToPercentage(double value) {
		return Percentage.valueOf((int) (100 - (200 * Math.abs(0.5 - value))));
	}
}
