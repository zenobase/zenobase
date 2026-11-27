package com.zenobase.json;

import com.zenobase.common.Pace;
import com.zenobase.common.Units;

public class PaceField extends DecimalMeasureField<Pace> {

	public PaceField(String name) {
		super(name, Units.S_PER_KM);
	}
}
