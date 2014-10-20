package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;

import org.junit.Test;

import com.zenobase.common.Units;

public class VolumeFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new VolumeField(FIELD_NAME), DecimalMeasure.valueOf(new BigDecimal("4.0"), Units.L));
	}
}
