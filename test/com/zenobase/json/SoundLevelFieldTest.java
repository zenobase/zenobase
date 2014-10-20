package com.zenobase.json;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Dimensionless;

import org.junit.Test;

public class SoundLevelFieldTest extends DecimalMeasureFieldTestSupport<Dimensionless> {

	@Override
	protected Field<DecimalMeasure<Dimensionless>> newField(String name) {
		return new SoundField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(valueOf("40 dB"));
	}
}
