package com.zenobase.json;

import com.zenobase.common.Units;
import javax.measure.quantity.Velocity;

public class VelocityField extends DecimalMeasureField<Velocity> {

	public VelocityField(String name) {
		super(name, Units.M_PER_S);
	}
}
