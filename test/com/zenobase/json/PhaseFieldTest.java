package com.zenobase.json;

import java.math.BigDecimal;

import org.junit.Test;

import com.zenobase.models.Phase;

public class PhaseFieldTest extends FieldTestSupport {

	private final PhaseField field = new PhaseField(FIELD_NAME);

	@Test
	public void test() {
		roundtrip(field, Phase.valueOf(new BigDecimal("0")));
		roundtrip(field, Phase.valueOf(new BigDecimal("0.0")));
		roundtrip(field, Phase.valueOf(new BigDecimal("0.9")));
	}
}
