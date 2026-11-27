package com.zenobase.json;

import com.zenobase.common.Units;
import javax.measure.quantity.DataAmount;

public class BitsField extends DecimalMeasureField<DataAmount> {

	public BitsField(String name) {
		super(name, Units.BIT);
	}
}
