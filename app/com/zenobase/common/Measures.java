package com.zenobase.common;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Dimension;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;

public class Measures {

	private Measures() {

	}

	@SuppressWarnings("unchecked")
	public static <Q extends Quantity> DecimalMeasure<Q> toStandard(DecimalMeasure<Q> measure) {
		return isStandard(measure.getUnit()) ? measure : measure.to((Unit<Q>) measure.getUnit().getStandardUnit(), MathContext.DECIMAL32);
	}

	public static boolean isStandard(Unit<?> unit) {
		return unit.isStandardUnit() || NonSI.DECIBEL.equals(unit);
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
		Unit<Q> u = Units.valueOf(unit);
		return valueOf(value, u);
	}

	public static <Q extends Quantity> DecimalMeasure<Q> valueOf(BigDecimal value, Unit<Q> unit) {
		return DecimalMeasure.valueOf(value, unit);
	}

	public static <Q extends Quantity> DecimalMeasure<Q> valueOf(String s) {
		return DecimalMeasure.valueOf(s);
	}

	public static BigDecimal convert(double value, Unit<?> unit) {
		return round(isStandard(unit) ? value : unit.getStandardUnit().getConverterTo(unit).convert(value));
	}

	public static BigDecimal round(double value) {
		return Doubles.isFinite(value) ? round(new BigDecimal(value)) : null;
	}

	public static BigDecimal round(BigDecimal value) {
		return value != null ? value.setScale(2, RoundingMode.HALF_UP) : null;
	}

	public static <Q extends Quantity> DecimalMeasure<Q> round(DecimalMeasure<Q> value) {
		return value != null ? new DecimalMeasure<Q>(round(value.getValue()), value.getUnit()) : null;
	}
}
