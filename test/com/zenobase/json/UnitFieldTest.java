package com.zenobase.json;

import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import org.junit.Test;

import com.zenobase.common.Units;

public class UnitFieldTest extends FieldTestSupport<Unit<Length>> {

	@Override
	protected Field<Unit<Length>> newField(String name) {
		return new UnitField<Length>(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(Units.MI);
	}
}
