package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.unit.NonSI;

import org.junit.Test;

public class TemperatureFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new TemperatureField(FIELD_NAME), DecimalMeasure.valueOf(new BigDecimal("70"), NonSI.FAHRENHEIT));
	}
}
