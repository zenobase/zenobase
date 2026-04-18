package com.zenobase.json;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Volume;
import org.junit.jupiter.api.Test;

public class VolumeFieldTest extends DecimalMeasureFieldTestSupport<Volume> {

	@Override
	protected Field<DecimalMeasure<Volume>> newField(String name) {
		return new VolumeField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(valueOf("4.0 L"));
		roundtrip(valueOf("1 gal"));
	}
}
