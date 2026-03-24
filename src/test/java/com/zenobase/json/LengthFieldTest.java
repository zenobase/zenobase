package com.zenobase.json;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;

import org.junit.Test;

public class LengthFieldTest extends DecimalMeasureFieldTestSupport<Length> {

	@Override
	protected Field<DecimalMeasure<Length>> newField(String name) {
		return new LengthField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(valueOf("1000 m"));
		roundtrip(valueOf("1.2345 mi"));
	}
}
