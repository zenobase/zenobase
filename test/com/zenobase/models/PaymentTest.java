package com.zenobase.models;

import java.math.BigDecimal;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

public class PaymentTest {

	@Test
	public void testEqualsHashCode() {
		new EqualsTester()
			.addEqualityGroup(new Payment(new BigDecimal("5.00")), new Payment(new BigDecimal("5.00")))
			.addEqualityGroup(new Payment(new BigDecimal("5.00"), "4111 1111 1111 1111", "100", "2015", "01"), new Payment(new BigDecimal("5.00"), "4111 1111 1111 1111", "100", "2015", "01"))
			.addEqualityGroup(new Payment(new BigDecimal("5.00"), "4111 1111 1111 1111", "100", "2015", "02"))
			.addEqualityGroup(new Payment(new BigDecimal("10.00")))
			.testEquals();
	}
}
