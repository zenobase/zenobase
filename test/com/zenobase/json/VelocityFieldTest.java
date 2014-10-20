package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;

import org.junit.Test;

import com.zenobase.common.Units;

public class VelocityFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new VelocityField(FIELD_NAME), DecimalMeasure.valueOf(new BigDecimal("100"), Units.MPH));
	}
}
