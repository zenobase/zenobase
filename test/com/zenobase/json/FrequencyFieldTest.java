package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.unit.SI;

import org.junit.Test;

public class FrequencyFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new FrequencyField(FIELD_NAME), DecimalMeasure.valueOf(new BigDecimal("60"), SI.HERTZ.divide(60L)));
	}
}
