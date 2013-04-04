package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.unit.NonSI;

import org.junit.Test;

public class SoundLevelFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new SoundLevelField(FIELD_NAME), DecimalMeasure.valueOf(new BigDecimal("40"), NonSI.DECIBEL));
	}
}
