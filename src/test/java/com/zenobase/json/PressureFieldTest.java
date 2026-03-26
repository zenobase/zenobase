package com.zenobase.json;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Pressure;

import org.junit.jupiter.api.Test;

public class PressureFieldTest extends DecimalMeasureFieldTestSupport<Pressure> {

	@Override
	protected Field<DecimalMeasure<Pressure>> newField(String name) {
		return new PressureField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(valueOf("75.0 Pa"));
		roundtrip(valueOf("40 psi"));
	}
}
