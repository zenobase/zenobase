package com.zenobase.json;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Velocity;
import org.junit.jupiter.api.Test;

public class PaceFieldTest extends DecimalMeasureFieldTestSupport<Velocity> {

	@Override
	protected Field<DecimalMeasure<Velocity>> newField(String name) {
		return new VelocityField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(valueOf("1 s/m"));
		roundtrip(valueOf("600 s/km"));
		roundtrip(valueOf("600.5 s/mi"));
	}
}
