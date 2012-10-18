package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.DataAmount;

import org.junit.Test;

import com.zenobase.common.Measures;

public class BitsFieldTest extends FieldTestSupport {

	@Test
	public void test() {

		DecimalMeasure<DataAmount> value = Measures.valueOf(new BigDecimal("1.2"), "GiB");
		roundtrip(new BitsField(FIELD_NAME), value);
	}
}
