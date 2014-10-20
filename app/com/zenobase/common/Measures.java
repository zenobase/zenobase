package com.zenobase.common;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.google.common.primitives.Doubles;

public class Measures {

	private Measures() {

	}

	public static <Q extends Quantity> DecimalMeasure<Q> valueOf(String s) {
		return DecimalMeasure.valueOf(s);
	}

	public static <Q extends Quantity> DecimalMeasure<Q> valueOf(BigDecimal value, String unit) {
		Unit<Q> u = Units.valueOf(unit);
		return valueOf(value, u);
	}

	public static <Q extends Quantity> DecimalMeasure<Q> valueOf(BigDecimal value, Unit<Q> unit) {
		return DecimalMeasure.valueOf(value, unit);
	}

	@SuppressWarnings("unchecked")
	public static <Q extends Quantity> DecimalMeasure<Q> toStandard(DecimalMeasure<Q> measure) {
		return Units.isStandard(measure.getUnit()) ? measure : measure.to((Unit<Q>) measure.getUnit().getStandardUnit(), MathContext.DECIMAL32);
	}

	public static BigDecimal convert(double value, Unit<?> unit) {
		return round(Units.isStandard(unit) ? value : unit.getStandardUnit().getConverterTo(unit).convert(value));
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
