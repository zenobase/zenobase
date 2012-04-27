package com.zenobase.json;

import java.math.BigDecimal;

import org.junit.Test;

import com.zenobase.json.LocationField;
import com.zenobase.models.Location;

public class LocationFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new LocationField(FIELD_NAME), new Location(new BigDecimal("47.6097"), new BigDecimal("-122.3331")));
	}
}
