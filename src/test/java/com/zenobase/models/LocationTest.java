package com.zenobase.models;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.testing.EqualsTester;
import org.junit.jupiter.api.Test;

public class LocationTest {

	@Test
	public void testEqualsHashCode() {
		Location l1 = new Location("47.6097", "-122.3331");
		Location l2 = new Location("47.6097", "-122");
		Location l3 = new Location("47", "-122");
		Location l4 = new Location("48.8742", "2.3470");
		new EqualsTester()
			.addEqualityGroup(l1, new Location(l1.latitude(), l1.longitude()))
			.addEqualityGroup(l2)
			.addEqualityGroup(l3)
			.addEqualityGroup(l4)
			.testEquals();
	}

	@Test
	public void testIsValid() {
		new Location("0", "0");
		new Location("90", "180");
		new Location("-90", "-180");
	}

	@Test
	public void testLatitudeOutOfRange() {
		assertThatThrownBy(() -> new Location("91", "0")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testLongitudeOutOfRange() {
		assertThatThrownBy(() -> new Location("0", "181")).isInstanceOf(IllegalArgumentException.class);
	}
}
