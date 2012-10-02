package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.unit.NonSI;

import org.junit.Test;

public class VelocityFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new VelocityField(FIELD_NAME), DecimalMeasure.valueOf(new BigDecimal("100"), NonSI.MILES_PER_HOUR));
	}
}
