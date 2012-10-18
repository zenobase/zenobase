package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.unit.NonSI;

import org.junit.Test;

public class VolumeFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new VolumeField(FIELD_NAME), DecimalMeasure.valueOf(new BigDecimal("4.0"), NonSI.LITER));
	}
}
