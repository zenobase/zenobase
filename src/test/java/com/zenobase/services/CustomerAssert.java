package com.zenobase.services;

import java.math.BigDecimal;

import com.braintreegateway.Customer;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.AbstractAssert;

import com.zenobase.models.Plan;

public class CustomerAssert extends AbstractAssert<CustomerAssert, Customer> {

	private CustomerAssert(Customer actual) {
		super(actual, CustomerAssert.class);
	}

	public CustomerAssert hasId(String id) {
		isNotNull();
		Assertions.assertThat(actual.getId()).isEqualTo(id);
		return this;
	}

	public CustomerAssert hasEmail(String email) {
		isNotNull();
		Assertions.assertThat(actual.getEmail()).isEqualTo(email);
		return this;
	}

	public CustomerAssert hasPaymentMethod() {
		isNotNull();
		Assertions.assertThat(actual.getDefaultPaymentMethod().getToken()).hasSize(6);
		return this;
	}

	public CustomerAssert planIsNull() {
		isNotNull();
		Assertions.assertThat(PaymentGateway.getSubscription(actual)).isNull();
		return this;
	}

	public CustomerAssert hasPlan(Plan plan) {
		isNotNull();
		Assertions.assertThat(PaymentGateway.getSubscription(actual).getPlanId()).isEqualTo(plan.getId());
		return this;
	}

	public CustomerAssert hasPrice(String price) {
		isNotNull();
		Assertions.assertThat(PaymentGateway.getSubscription(actual).getPrice()).isEqualTo(new BigDecimal(price));
		return this;
	}

	public static CustomerAssert assertThat(Customer actual) {
		return new CustomerAssert(actual);
	}
}
