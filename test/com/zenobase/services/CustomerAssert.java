package com.zenobase.services;

import org.fest.assertions.Assertions;
import org.fest.assertions.GenericAssert;
import com.braintreegateway.Customer;

public class CustomerAssert extends GenericAssert<CustomerAssert, Customer> {

	private CustomerAssert(Customer actual) {
		super(CustomerAssert.class, actual);
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

	public CustomerAssert hasCardEndingWith(String last4) {
		isNotNull();
		Assertions.assertThat(PaymentGateway.getCreditCard(actual).getLast4()).isEqualTo(last4);
		return this;
	}

	public CustomerAssert hasPlan(String planId) {
		isNotNull();
		Assertions.assertThat(PaymentGateway.getSubscription(actual).getPlanId()).isEqualTo(planId);
		return this;
	}

	public static CustomerAssert assertThat(Customer actual) {
		return new CustomerAssert(actual);
	}
}
