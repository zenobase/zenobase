package com.zenobase.json;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Illuminance;

import org.junit.Test;

public class LightFieldTest extends DecimalMeasureFieldTestSupport<Illuminance> {

	@Override
	protected Field<DecimalMeasure<Illuminance>> newField(String name) {
		return new LightField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(valueOf("100 lx"));
	}
}
