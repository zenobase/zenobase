package com.zenobase.json;

import org.junit.Test;

import com.zenobase.models.Location;

public class LocationFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new LocationField(FIELD_NAME), new Location("47.6097", "-122.3331"));
	}
}
