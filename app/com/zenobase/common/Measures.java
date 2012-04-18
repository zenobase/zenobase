package com.zenobase.common;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

public class Measures {

	static {
		UnitFormat.getInstance().label(SI.CELSIUS, "°C");
	}

	private Measures() {
		
	}

	public static <Q extends Quantity> DecimalMeasure<Q> toStandard(DecimalMeasure<Q> measure) {
		return measure.to((Unit<Q>) measure.getUnit().getStandardUnit());
	}
}
