package com.zenobase.json;

import com.zenobase.common.Units;
import javax.measure.quantity.Pressure;

public class PressureField extends DecimalMeasureField<Pressure> {

	public PressureField(String name) {
		super(name, Units.PA);
	}
}
