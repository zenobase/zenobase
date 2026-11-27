package com.zenobase.json;

import com.zenobase.common.LengthPerVolume;
import com.zenobase.common.Units;

public class LengthPerVolumeField extends DecimalMeasureField<LengthPerVolume> {

	public LengthPerVolumeField(String name) {
		super(name, Units.KPL);
	}
}
