package com.zenobase.json;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.VolumetricDensity;

import org.junit.Test;

import com.zenobase.common.Measures;

public class ConcentrationFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		DecimalMeasure<VolumetricDensity> value = Measures.valueOf(new BigDecimal("10.0"), "ng/dL");
		roundtrip(new ConcentrationField(FIELD_NAME), value);
	}
}
