package com.zenobase.json;

import com.zenobase.common.Units;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;
import org.junit.jupiter.api.Test;

public class UnitFieldTest extends FieldTestSupport<Unit<Length>> {

	@Override
	protected Field<Unit<Length>> newField(String name) {
		return new UnitField<>(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(Units.MI);
	}
}
