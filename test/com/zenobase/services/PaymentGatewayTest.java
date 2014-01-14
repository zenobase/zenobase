package com.zenobase.services;

import static com.zenobase.services.CustomerAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import com.braintreegateway.Environment;

import com.zenobase.models.Card;

public class PaymentGatewayTest {

	private static final String USERNAME = "jdoe";
	private static final String EMAIL = "jdoe@zenobase.com";
	private static final Card CARD = new Card("4111 1111 1111 1111", "100", "2015", "01");
	private static final String PLAN_1K = "1000";
	private static final String PLAN_50K = "50000";
	private static final String PLAN_3M = "3000000";

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
	@Ignore
	public void testNewSubscription() {
		assertThat(gateway.find(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, CARD, PLAN_50K);
		assertThat(gateway.find(USERNAME)).hasId(USERNAME).hasEmail(EMAIL).hasCardEndingWith("1111").hasPlan(PLAN_50K);
		assertThat(gateway.cancel(USERNAME)).isTrue();
		assertThat(gateway.cancel(USERNAME)).isFalse();
		assertThat(gateway.find(USERNAME)).isNull();
	}

	@Test(expected = IllegalArgumentException.class)
	@Ignore
	public void testNewSubscriptionWithInvalidCard() {
		Card invalid = new Card("4000 1111 1111 1115", "100", "15", "01");
		assertThat(gateway.find(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, invalid, PLAN_50K);
	}

	@Test
	@Ignore
	public void testUpgradeSubscriptionWithExistingCard() {
		assertThat(gateway.find(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, CARD, PLAN_50K);
		gateway.subscribe(USERNAME, EMAIL, null, PLAN_3M);
		assertThat(gateway.find(USERNAME)).hasId(USERNAME).hasEmail(EMAIL).hasCardEndingWith("1111").hasPlan(PLAN_3M);
		assertThat(gateway.cancel(USERNAME)).isTrue();
	}

	@Test
	@Ignore
	public void testDowngradeSubscriptionWithExistingCard() {
		assertThat(gateway.find(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, CARD, PLAN_50K);
		gateway.subscribe(USERNAME, EMAIL, null, PLAN_1K);
		assertThat(gateway.find(USERNAME)).hasId(USERNAME).hasEmail(EMAIL).hasCardEndingWith("1111").hasPlan(PLAN_1K);
		assertThat(gateway.cancel(USERNAME)).isTrue();
	}

	@Test
	@Ignore
	public void testUpgradeSubscriptionWithNewCard() {
		Card newCard = new Card("4005 5192 0000 0004", "101", "2016", "01");
		assertThat(gateway.find(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, CARD, PLAN_50K);
		gateway.subscribe(USERNAME, EMAIL, newCard, PLAN_3M);
		assertThat(gateway.find(USERNAME)).hasId(USERNAME).hasEmail(EMAIL).hasCardEndingWith("0004").hasPlan(PLAN_3M);
		assertThat(gateway.cancel(USERNAME)).isTrue();
	}

	@Test
	@Ignore
	public void testUpgradeSubscriptionWithInvalidCard() {
		Card invalid = new Card("4000 1111 1111 1115", "100", "15", "01");
		assertThat(gateway.find(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, CARD, PLAN_50K);
		try {
			gateway.subscribe(USERNAME, EMAIL, invalid, PLAN_50K);
		} catch (IllegalArgumentException e) {

		}
		assertThat(gateway.find(USERNAME)).hasId(USERNAME).hasEmail(EMAIL).hasCardEndingWith("1111").hasPlan(PLAN_50K);
		assertThat(gateway.cancel(USERNAME)).isTrue();
	}
}
