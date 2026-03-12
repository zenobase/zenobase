package com.zenobase.models;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class LocationTest {

	@Test
	public void testEqualsHashCode() {
		Location l1 = new Location("47.6097", "-122.3331");
		Location l2 = new Location("47.6097", "-122");
		Location l3 = new Location("47", "-122");
		Location l4 = new Location("48.8742", "2.3470");
		new EqualsTester()
			.addEqualityGroup(l1, new Location(l1.getLatitude(), l1.getLongitude()))
			.addEqualityGroup(l2).addEqualityGroup(l3)
			.addEqualityGroup(l4).testEquals();
	}

	@Test
	public void testIsValid() {
		assertTrue(new Location("0", "0").isValid());
		assertTrue(new Location("90", "180").isValid());
		assertTrue(new Location("-90", "-180").isValid());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOutOfRangeLatitude() {
		new Location("91", "0");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOutOfRangeLongitude() {
		new Location("0", "181");
	}
}
