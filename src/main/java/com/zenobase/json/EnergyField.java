package com.zenobase.json;

import com.zenobase.common.Units;
import javax.measure.quantity.Energy;

public class EnergyField extends DecimalMeasureField<Energy> {

	public EnergyField(String name) {
		super(name, Units.J);
	}
}
