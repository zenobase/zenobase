package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.unit.SI;

import org.junit.Test;

public class EnergyFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new EnergyField(FIELD_NAME), DecimalMeasure.valueOf(new BigDecimal("1"), SI.JOULE.times(1000)));
	}
}
