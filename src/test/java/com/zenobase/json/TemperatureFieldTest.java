package com.zenobase.json;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Temperature;
import org.junit.jupiter.api.Test;

public class TemperatureFieldTest extends DecimalMeasureFieldTestSupport<Temperature> {

	@Override
	protected Field<DecimalMeasure<Temperature>> newField(String name) {
		return new TemperatureField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(valueOf("20.5 C"));
		roundtrip(valueOf("70.0 F"));
		roundtrip(valueOf("300 K"));
	}
}
