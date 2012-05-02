package com.zenobase.models;

import java.math.BigDecimal;

import org.junit.Test;
import com.google.common.testing.EqualsTester;

public class LocationTest {

	@Test
	public void testEqualsHashCode() {
		Location l1 = new Location(new BigDecimal("47.6097"), new BigDecimal("-122.3331"));
		Location l2 = new Location(new BigDecimal("47.6097"), new BigDecimal("-122"));
		Location l3 = new Location(new BigDecimal("47"), new BigDecimal("-122"));
		Location l4 = new Location(new BigDecimal("48.8742"), new BigDecimal("2.3470"));
		new EqualsTester()
			.addEqualityGroup(l1, new Location(l1.getLatitude(), l1.getLongitude()))
			.addEqualityGroup(l2).addEqualityGroup(l3)
			.addEqualityGroup(l4).testEquals();
	}
}
