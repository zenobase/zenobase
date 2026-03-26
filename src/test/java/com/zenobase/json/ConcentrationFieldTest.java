package com.zenobase.json;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.VolumetricDensity;

import org.junit.jupiter.api.Test;

public class ConcentrationFieldTest extends DecimalMeasureFieldTestSupport<VolumetricDensity> {

	@Override
	protected Field<DecimalMeasure<VolumetricDensity>> newField(String name) {
		return new ConcentrationField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(valueOf("10.0 ng/dL"));
	}
}
