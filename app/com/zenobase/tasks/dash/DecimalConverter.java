package com.zenobase.tasks.dash;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.google.common.base.Function;

import com.zenobase.common.Measures;

class DecimalConverter<Q extends Quantity> implements Function<BigDecimal, DecimalMeasure<Q>> {

	private final Unit<Q> unit;

	public DecimalConverter(Unit<Q> unit) {
		this.unit = unit;
	}

	@Override
	public DecimalMeasure<Q> apply(BigDecimal value) {
		return Measures.valueOf(value, unit);
	}
}
