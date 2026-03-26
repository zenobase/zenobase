package com.zenobase.services;

import static com.zenobase.services.CustomerAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.braintreegateway.Environment;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zenobase.models.Payment;
import com.zenobase.models.Plan;

public class PaymentGatewayTesting {

	private static final String USERNAME = "jdoe";
	private static final String EMAIL = "jdoe@zenobase.com";
	private static final Payment PAYMENT = new Payment(Plan.PERSONAL.getPrice(), "fake-valid-visa-nonce");

	private PaymentGateway gateway;

	@BeforeEach
	public void setUp() {
		String merchantId = System.getProperty("braintree.merchant_id");
		String publicKey = System.getProperty("braintree.public_key");
		String privateKey = System.getProperty("braintree.private_key");
		Assumptions.assumeTrue(merchantId != null && publicKey != null && privateKey != null);
		gateway = new PaymentGateway(Environment.SANDBOX, merchantId, publicKey, privateKey);
	}

	@Test
	public void testNewSubscription() {
		assertThat(gateway.findCustomer(USERNAME)).isNull();
		assertThat(gateway.token(USERNAME)).isNotNull();
		gateway.subscribe(USERNAME, EMAIL, PAYMENT, Plan.PERSONAL);
		assertThat(gateway.findCustomer(USERNAME))
				.hasId(USERNAME)
				.hasEmail(EMAIL)
				.hasPaymentMethod()
				.hasPlan(Plan.PERSONAL)
				.hasPrice("5.00");
		assertThat(gateway.cancel(USERNAME)).isTrue();
		assertThat(gateway.cancel(USERNAME)).isFalse();
		assertThat(gateway.findCustomer(USERNAME)).isNull();
	}

	@Test
	public void testNewSubscriptionWithInvalidCard() {
		Payment invalid = new Payment(Plan.PERSONAL.getPrice(), "fake-processor-declined-visa-nonce");
		assertThat(gateway.findCustomer(USERNAME)).isNull();
		assertThatThrownBy(() -> gateway.subscribe(USERNAME, EMAIL, invalid, Plan.PERSONAL))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testUpdateSubscriptionWithNewCard() {
		Payment newPayment = new Payment(Plan.PERSONAL.getPrice(), "fake-valid-amex-nonce");
		assertThat(gateway.findCustomer(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, PAYMENT, Plan.PERSONAL);
		assertThat(gateway.token(USERNAME)).isNotNull();
		gateway.subscribe(USERNAME, EMAIL, newPayment, Plan.PERSONAL);
		assertThat(gateway.findCustomer(USERNAME))
				.hasId(USERNAME)
				.hasEmail(EMAIL)
				.hasPaymentMethod()
				.hasPlan(Plan.PERSONAL)
				.hasPrice("5.00");
		assertThat(gateway.cancel(USERNAME)).isTrue();
	}

	@Test
	public void testUpdateSubscriptionWithInvalidCard() {
		Payment invalid = new Payment(Plan.PERSONAL.getPrice(), "fake-processor-declined-visa-nonce");
		assertThat(gateway.findCustomer(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, PAYMENT, Plan.PERSONAL);
		try {
			gateway.subscribe(USERNAME, EMAIL, invalid, Plan.PERSONAL);
		} catch (IllegalArgumentException e) {

		}
		assertThat(gateway.findCustomer(USERNAME))
				.hasId(USERNAME)
				.hasEmail(EMAIL)
				.hasPaymentMethod()
				.hasPlan(Plan.PERSONAL)
				.hasPrice("5.00");
		assertThat(gateway.cancel(USERNAME)).isTrue();
	}

	@Test
	public void testCancelSubscription() {
		assertThat(gateway.findCustomer(USERNAME)).isNull();
		gateway.subscribe(USERNAME, EMAIL, PAYMENT, Plan.PERSONAL);
		gateway.unsubscribe(USERNAME);
		assertThat(gateway.findCustomer(USERNAME))
				.hasId(USERNAME)
				.hasEmail(EMAIL)
				.hasPaymentMethod()
				.planIsNull();
		assertThat(gateway.cancel(USERNAME)).isTrue();
	}
}
