package com.zenobase.json;

import com.zenobase.common.Units;
import javax.measure.quantity.Length;

public class LengthField extends DecimalMeasureField<Length> {

	public LengthField(String name) {
		super(name, Units.M);
	}
}
