package com.zenobase.json;

import com.zenobase.common.Units;
import javax.measure.quantity.Mass;

public class WeightField extends DecimalMeasureField<Mass> {

	public WeightField(String name) {
		super(name, Units.KG);
	}
}
