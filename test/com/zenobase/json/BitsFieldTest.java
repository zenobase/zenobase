package com.zenobase.json;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.DataAmount;

import org.junit.Test;

public class BitsFieldTest extends DecimalMeasureFieldTestSupport<DataAmount> {

	@Override
	protected Field<DecimalMeasure<DataAmount>> newField(String name) {
		return new BitsField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(valueOf("1 GB"));
		roundtrip(valueOf("1.2 GiB"));
	}
}
