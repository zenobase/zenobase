package com.zenobase.json;

import com.zenobase.common.Units;
import javax.measure.quantity.Volume;

public class VolumeField extends DecimalMeasureField<Volume> {

	public VolumeField(String name) {
		super(name, Units.L);
	}
}
