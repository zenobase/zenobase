package com.zenobase.json;

import org.junit.Test;

import com.zenobase.models.Location;

public class LocationFieldTest extends FieldTestSupport<Location> {

	@Override
	protected Field<Location> newField(String name) {
		return new LocationField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(new Location("47.6097", "-122.3331"));
	}
}
