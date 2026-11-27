package com.zenobase.json;

import com.zenobase.common.Units;
import javax.measure.quantity.Dimensionless;

public class SoundField extends DecimalMeasureField<Dimensionless> {

	public SoundField(String name) {
		super(name, Units.DB);
	}
}
