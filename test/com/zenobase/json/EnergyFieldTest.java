package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;

import org.junit.Test;

import com.zenobase.common.Units;

public class EnergyFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new EnergyField(FIELD_NAME), DecimalMeasure.valueOf(new BigDecimal("1"), Units.KJ));
	}
}
