package com.zenobase.json;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Velocity;

import org.junit.jupiter.api.Test;

public class VelocityFieldTest extends DecimalMeasureFieldTestSupport<Velocity> {

	@Override
	protected Field<DecimalMeasure<Velocity>> newField(String name) {
		return new VelocityField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(valueOf("10.0 m/s"));
		roundtrip(valueOf("120 kmh"));
		roundtrip(valueOf("100 mph"));
	}
}
