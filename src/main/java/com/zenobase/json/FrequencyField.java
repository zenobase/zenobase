package com.zenobase.json;

import com.zenobase.common.Units;
import javax.measure.quantity.Frequency;

public class FrequencyField extends DecimalMeasureField<Frequency> {

	public FrequencyField(String name) {
		super(name, Units.HZ);
	}
}
