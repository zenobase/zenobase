package com.zenobase.json;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;

public abstract class DecimalMeasureFieldTestSupport<Q extends Quantity> extends FieldTestSupport<DecimalMeasure<Q>> {

	protected DecimalMeasure<Q> valueOf(String s) {
		return DecimalMeasure.<Q>valueOf(s);
	}
}
