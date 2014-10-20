package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;

import org.junit.Test;

import com.zenobase.common.Units;

public class PressureFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new PressureField(FIELD_NAME), DecimalMeasure.valueOf(new BigDecimal("75.0"), Units.PA));
	}
}
