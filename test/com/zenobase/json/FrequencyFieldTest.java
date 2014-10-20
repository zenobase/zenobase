package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;

import org.junit.Test;

import com.zenobase.common.Units;

public class FrequencyFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new FrequencyField(FIELD_NAME), DecimalMeasure.valueOf(new BigDecimal("60"), Units.BPM));
	}
}
