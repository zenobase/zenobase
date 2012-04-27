package com.zenobase.schema;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.unit.SI;

import org.junit.Test;

public class LengthFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new LengthField(FIELD_NAME), DecimalMeasure.valueOf(new BigDecimal("1.2345"), SI.KILOMETER));
	}
}
