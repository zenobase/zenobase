package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;

import org.junit.Test;

import com.zenobase.common.Units;

public class LengthFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new LengthField(FIELD_NAME), DecimalMeasure.valueOf(new BigDecimal("1.2345"), Units.MI));
	}
}
