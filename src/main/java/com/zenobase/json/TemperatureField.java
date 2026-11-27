package com.zenobase.json;

import com.zenobase.common.Units;
import javax.measure.quantity.Temperature;

public class TemperatureField extends DecimalMeasureField<Temperature> {

	public TemperatureField(String name) {
		super(name, Units.K);
	}
}
