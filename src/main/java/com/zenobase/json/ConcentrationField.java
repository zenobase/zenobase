package com.zenobase.json;

import com.zenobase.common.Units;
import javax.measure.quantity.VolumetricDensity;

public class ConcentrationField extends DecimalMeasureField<VolumetricDensity> {

	public ConcentrationField(String name) {
		super(name, Units.G_PER_L);
	}
}
