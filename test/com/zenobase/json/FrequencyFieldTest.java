package com.zenobase.json;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Frequency;

import org.junit.Test;

public class FrequencyFieldTest extends DecimalMeasureFieldTestSupport<Frequency> {

	@Override
	protected Field<DecimalMeasure<Frequency>> newField(String name) {
		return new FrequencyField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(valueOf("100 Hz"));
		roundtrip(valueOf("60 bpm"));
	}
}
