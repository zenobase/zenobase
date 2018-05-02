package com.zenobase.json;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;

import org.junit.Test;

public class EnergyFieldTest extends DecimalMeasureFieldTestSupport<Energy> {

	@Override
	protected Field<DecimalMeasure<Energy>> newField(String name) {
		return new EnergyField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(valueOf("1 kJ"));
		roundtrip(valueOf("5000 kcal"));
		roundtrip(valueOf("1.5 kWh"));
	}
}
