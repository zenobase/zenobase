package com.zenobase.services;

import static com.zenobase.services.CustomerAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import com.braintreegateway.Environment;

import com.zenobase.models.Payment;
import com.zenobase.models.Plan;

public class PaymentGatewayTest {

	private static final String USERNAME = "jdoe";
	private static final String EMAIL = "jdoe@zenobase.com";
	private static final Payment PAYMENT = new Payment(BigDecimal.valueOf(5), "4111 1111 1111 1111", "100", "2015", "01");

	private PaymentGateway gateway;

	@Before
	public void setUp() {
		String merchantId = System.getProperty("braintree.merchant_id");
		String publicKey = System.getProperty("braintree.public_key");
		String privateKey = System.getProperty("braintree.private_key");
		Assume.assumeNotNull(merchantId, publicKey, privateKey);
		gateway = new PaymentGateway(Environment.SANDBOX, merchantId, publicKey, privateKey);
	}

	@Test
	public void testNewSubscription() {
		assertThat(gateway.findCustomer(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, PAYMENT, Plan.PERSONAL);
		assertThat(gateway.findCustomer(USERNAME)).hasId(USERNAME).hasEmail(EMAIL).hasCardEndingWith("1111").hasPlan(Plan.PERSONAL).hasPrice("5.00");
		assertThat(gateway.cancel(USERNAME)).isTrue();
		assertThat(gateway.cancel(USERNAME)).isFalse();
		assertThat(gateway.findCustomer(USERNAME)).isNull();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNewSubscriptionWithInvalidCard() {
		Payment invalid = new Payment(BigDecimal.valueOf(5), "4000 1111 1111 1115", "100", "15", "01");
		assertThat(gateway.findCustomer(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, invalid, Plan.PERSONAL);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNewSubscriptionWithInvalidPrice() {
		Payment invalid = new Payment(BigDecimal.valueOf(-5), "4000 1111 1111 1111", "100", "2015", "01");
		assertThat(gateway.findCustomer(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, invalid, Plan.PERSONAL);
	}

	@Test
	public void testUpgradeSubscriptionWithExistingCard() {
		assertThat(gateway.findCustomer(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, PAYMENT, Plan.PERSONAL);
		gateway.subscribe(USERNAME, EMAIL, new Payment(BigDecimal.TEN), Plan.PERSONAL);
		assertThat(gateway.findCustomer(USERNAME)).hasId(USERNAME).hasEmail(EMAIL).hasCardEndingWith("1111").hasPlan(Plan.PERSONAL).hasPrice("10.00");
		assertThat(gateway.cancel(USERNAME)).isTrue();
	}

	@Test
	public void testDowngradeSubscriptionWithExistingCard() {
		assertThat(gateway.findCustomer(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, PAYMENT, Plan.PERSONAL);
		gateway.subscribe(USERNAME, EMAIL, new Payment(BigDecimal.ZERO), Plan.PERSONAL);
		assertThat(gateway.findCustomer(USERNAME)).hasId(USERNAME).hasEmail(EMAIL).hasCardEndingWith("1111").hasPlan(Plan.PERSONAL).hasPrice("0.00");
		assertThat(gateway.cancel(USERNAME)).isTrue();
	}

	@Test
	public void testUpgradeSubscriptionWithNewCard() {
		Payment newCard = new Payment(BigDecimal.TEN, "4005 5192 0000 0004", "101", "2016", "01");
		assertThat(gateway.findCustomer(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, PAYMENT, Plan.PERSONAL);
		gateway.subscribe(USERNAME, EMAIL, newCard, Plan.PERSONAL);
		assertThat(gateway.findCustomer(USERNAME)).hasId(USERNAME).hasEmail(EMAIL).hasCardEndingWith("0004").hasPlan(Plan.PERSONAL).hasPrice("10.00");
		assertThat(gateway.cancel(USERNAME)).isTrue();
	}

	@Test
	public void testUpgradeSubscriptionWithInvalidCard() {
		Payment invalid = new Payment(BigDecimal.TEN, "4000 1111 1111 1115", "100", "15", "01");
		assertThat(gateway.findCustomer(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, PAYMENT, Plan.PERSONAL);
		try {
			gateway.subscribe(USERNAME, EMAIL, invalid, Plan.PERSONAL);
		} catch (IllegalArgumentException e) {

		}
		assertThat(gateway.findCustomer(USERNAME)).hasId(USERNAME).hasEmail(EMAIL).hasCardEndingWith("1111").hasPlan(Plan.PERSONAL).hasPrice("5.00");
		assertThat(gateway.cancel(USERNAME)).isTrue();
	}

	@Test
	public void testCancelSubscription() {
		assertThat(gateway.findCustomer(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, PAYMENT, Plan.PERSONAL);
		gateway.unsubscribe(USERNAME);
		assertThat(gateway.findCustomer(USERNAME)).hasId(USERNAME).hasEmail(EMAIL).hasCardEndingWith("1111").planIsNull();
		assertThat(gateway.cancel(USERNAME)).isTrue();
	}
}
