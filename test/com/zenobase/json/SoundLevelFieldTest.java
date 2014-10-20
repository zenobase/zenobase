package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;

import org.junit.Test;

import com.zenobase.common.Units;

public class SoundLevelFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new SoundField(FIELD_NAME), DecimalMeasure.valueOf(new BigDecimal("40"), Units.DB));
	}
}
