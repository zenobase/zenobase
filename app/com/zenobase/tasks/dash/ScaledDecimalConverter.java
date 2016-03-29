package com.zenobase.tasks.dash;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

class ScaledDecimalConverter<Q extends Quantity> extends DecimalConverter<Q> {

	private final BigDecimal factor;

	public ScaledDecimalConverter(Unit<Q> unit, BigDecimal factor) {
		super(unit);
		this.factor = factor;
	}

	@Override
	public DecimalMeasure<Q> apply(BigDecimal value) {
		return super.apply(factor.multiply(value));
	}
}
