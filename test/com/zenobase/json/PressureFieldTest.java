package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.unit.SI;

import org.junit.Test;

public class PressureFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new PressureField(FIELD_NAME), DecimalMeasure.valueOf(new BigDecimal("75.0"), SI.PASCAL));
	}
}
