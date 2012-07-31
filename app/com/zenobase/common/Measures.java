package com.zenobase.common;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Dimension;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import com.google.common.collect.Lists;

public class Measures {

	static {
		UnitFormat.getInstance().label(SI.CELSIUS, "°C");
		UnitFormat.getInstance().alias(SI.CELSIUS, "C");
		UnitFormat.getInstance().alias(NonSI.FAHRENHEIT, "F");
		UnitFormat.getInstance().label(SI.HERTZ.divide(60L), "bpm");
	}

	private Measures() {
		throw new AssertionError();
	}

	public static <Q extends Quantity> DecimalMeasure<Q> toStandard(DecimalMeasure<Q> measure) {
		return measure.to((Unit<Q>) measure.getUnit().getStandardUnit(), MathContext.DECIMAL32);
	}

	public static <Q extends Quantity> Iterable<Unit<Q>> getUnits(Dimension dimension, Class<Q> type) {
		List<Unit<Q>> units = Lists.newArrayList();
		for (Unit<?> unit : SI.getInstance().getUnits()) {
			if (unit.getDimension().equals(dimension)) {
				units.add(unit.asType(type));
			}
		}
		for (Unit<?> unit : NonSI.getInstance().getUnits()) {
			if (unit.getDimension().equals(dimension)) {
				units.add(unit.asType(type));
			}
		}
		return units;
	}

	public static <Q extends Quantity> DecimalMeasure<Q> valueOf(BigDecimal value, String unit) {
		return DecimalMeasure.valueOf(value, (Unit<Q>) Unit.valueOf(unit));
	}

	public static long convert(double value, Unit<?> unit) {
		return Math.round(unit.getStandardUnit().getConverterTo(unit).convert(value));
	}
}
