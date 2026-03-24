package com.zenobase.json;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Mass;

import org.junit.Test;

public class WeightFieldTest extends DecimalMeasureFieldTestSupport<Mass> {

	@Override
	protected Field<DecimalMeasure<Mass>> newField(String name) {
		return new WeightField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(valueOf("70.0 kg"));
		roundtrip(valueOf("160 lb"));
	}
}
