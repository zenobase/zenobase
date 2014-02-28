package com.zenobase.tasks.forecast;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Quantity;
import javax.measure.quantity.Temperature;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Measures;

public class ForecastResult {

	private final ObjectNode node;
	private final boolean standardUnits;

	public ForecastResult(ObjectNode node, boolean standardUnits) {
		this.node = node;
		this.standardUnits = standardUnits;
	}

	public Forecast get(DateTime time) {
		return get(time.withMinuteOfHour(0).withSecondOfMinute(0).getMillis() / 1000L);
	}

	public Forecast get(long epochSeconds) {
		for (JsonNode hourNode : node.path("hourly").path("data")) {
			if (hourNode.path("time").longValue() == epochSeconds) {
				return parse(hourNode);
			}
		}
		return null;
	}

	private Forecast parse(JsonNode node) {
		String tag = node.path("summary").textValue();
		DecimalMeasure<Temperature> temperature = measureValue(node.path("temperature"), getTemperatureUnit());
		DecimalMeasure<Pressure> pressure = measureValue(node.path("pressure"), getPressureUnit());
		Integer humidity = percentValue(node.path("humidity"));
		return new Forecast(tag, temperature, pressure, humidity);
	}

	private Unit<Temperature> getTemperatureUnit() {
		return standardUnits ? SI.CELSIUS : NonSI.FAHRENHEIT;
	}

	private Unit<Pressure> getPressureUnit() {
		return SI.HECTO(SI.PASCAL);
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
}
