package com.zenobase.models;

import java.math.BigDecimal;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

public class PaymentTest {

	@Test
	public void testEqualsHashCode() {
		new EqualsTester()
			.addEqualityGroup(new Payment(new BigDecimal("5.00")), new Payment(new BigDecimal("5.00")))
			.addEqualityGroup(new Payment(new BigDecimal("5.00"), "xyz"), new Payment(new BigDecimal("5.00"), "xyz"))
			.addEqualityGroup(new Payment(new BigDecimal("5.00"), "abc"))
			.addEqualityGroup(new Payment(new BigDecimal("10.00")))
			.testEquals();
	}
}
