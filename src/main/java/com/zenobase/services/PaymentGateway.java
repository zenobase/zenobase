package com.zenobase.services;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.ClientTokenRequest;
import com.braintreegateway.CreditCard;
import com.braintreegateway.CreditCardVerification;
import com.braintreegateway.Customer;
import com.braintreegateway.CustomerRequest;
import com.braintreegateway.Environment;
import com.braintreegateway.PaymentMethod;
import com.braintreegateway.PaymentMethodRequest;
import com.braintreegateway.Result;
import com.braintreegateway.Subscription;
import com.braintreegateway.SubscriptionRequest;
import com.braintreegateway.ValidationError;
import com.braintreegateway.exceptions.NotFoundException;
import com.google.common.base.Preconditions;
import java.util.ArrayList;

import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.models.Payment;
import com.zenobase.models.Plan;

public class PaymentGateway {

	private static final Logger logger = LoggerFactory.getLogger(PaymentGateway.class);

	private final BraintreeGateway gateway;

	@Inject
	public PaymentGateway(@Named("braintree.merchant_id") String merchantId, @Named("braintree.public_key") String publicKey, @Named("braintree.private_key") String privateKey) {
		this(Environment.PRODUCTION, merchantId, publicKey, privateKey);
	}

	PaymentGateway(Environment environment, String merchantId, String publicKey, String privateKey) {
		gateway = new BraintreeGateway(environment, merchantId, publicKey, privateKey);
	}

	public Customer findCustomer(String username) {
		try {
			return gateway.customer().find(username);
		} catch (NotFoundException e) {
			return null;
		}
	}

	public void subscribe(String username, String email, Payment payment, Plan plan) {
		Customer customer = findCustomer(username);
		if (customer != null) {
			replaceSubscription(customer, payment, plan);
		} else {
			newSubscription(username, email, payment, plan);
		}
	}

	private void replaceSubscription(Customer customer, Payment payment, Plan plan) {
		Subscription subscription = getSubscription(customer);
		PaymentMethod paymentMethod = payment.getNonce() != null ? newPaymentMethod(customer.getId(), payment) : customer.getDefaultPaymentMethod();
		Preconditions.checkArgument(paymentMethod != null, "Expected a card for <%s>", customer.getId());
		var request = new SubscriptionRequest().paymentMethodToken(paymentMethod.getToken());
		if (subscription == null || subscription.getStatus() != Subscription.Status.PAST_DUE) {
			request = request.planId(plan.getId());
		}
		Result<Subscription> result = subscription != null
			? gateway.subscription().update(subscription.getId(), request)
			: gateway.subscription().create(request);
		Preconditions.checkArgument(result.isSuccess(), "Couldn't subscribe <%s> to <%s>: %s", customer.getId(), plan.getId(), result.getMessage());
	}

	private void newSubscription(String username, String email, Payment payment, Plan plan) {
		Preconditions.checkNotNull(payment, "Can't create customer <%s> without a card", username);
		Customer customer = newCustomer(username, email, payment);
		var request = new SubscriptionRequest().planId(plan.getId()).paymentMethodToken(customer.getDefaultPaymentMethod().getToken());
		Result<Subscription> result = gateway.subscription().create(request);
		Preconditions.checkArgument(result.isSuccess(), "Couldn't subscribe <%s> to <%s>: %s", username, plan.getId(), result.getMessage());
	}

	private Customer newCustomer(String username, String email, Payment payment) {
		var request = new CustomerRequest().id(username).email(email).paymentMethodNonce(payment.getNonce());
		Result<Customer> result = gateway.customer().create(request);
		Preconditions.checkArgument(result.isSuccess(), "Couldn't create customer <%s>: %s", username, result.getMessage());
		return result.getTarget();
	}

	private PaymentMethod newPaymentMethod(String username, Payment payment) {
		var request = new PaymentMethodRequest().customerId(username).paymentMethodNonce(payment.getNonce()).options().makeDefault(true).done();
		Result<? extends PaymentMethod> result = gateway.paymentMethod().create(request);
		Preconditions.checkArgument(result.isSuccess(), "Couldn't store credit card for <%s>: %s", username, result.getMessage());
		Preconditions.checkArgument(isVerified(result), "Couldn't verify credit card for <%s> (%s): %s", username, getStatus(result), getErrorMessage(result));
		return result.getTarget();
	}

	private static boolean isVerified(Result<? extends PaymentMethod> result) {
		return getStatus(result) == CreditCardVerification.Status.VERIFIED;
	}

	private static CreditCardVerification.Status getStatus(Result<? extends PaymentMethod> result) {
		return result.getCreditCardVerification() != null ? result.getCreditCardVerification().getStatus() : null;
	}

	private static String getErrorMessage(Result<? extends PaymentMethod> result) {
		var msg = new StringBuilder();
		if (result.getErrors() != null) {
			for (ValidationError error : result.getErrors().getAllValidationErrors()) {
				msg.append("[").append(error.getAttribute()).append("] ").append(error.getMessage()).append(" (").append(error.getCode()).append(")");
				break;
			}
		}
		return msg.toString();
	}

	static Subscription getSubscription(Customer customer) {
		List<Subscription> subscriptions = new ArrayList<>();
		for (CreditCard card : customer.getCreditCards()) {
			for (Subscription subscription : card.getSubscriptions()) {
				if (!Subscription.Status.CANCELED.equals(subscription.getStatus())) {
					subscriptions.add(subscription);
				}
			}
		}
		Preconditions.checkState(subscriptions.size() <= 1, "Expected at most one subscription for <%s> but got <%d>", customer.getId(), subscriptions.size());
		return Iterables.getOnlyElement(subscriptions, null);
	}

	public void unsubscribe(String username) {
		Customer customer = findCustomer(username);
		Preconditions.checkArgument(customer != null, "Couldn't find customer: <%s>", username);
		Subscription subscription = getSubscription(customer);
		Preconditions.checkArgument(subscription != null, "Expected at least one subscription for <%s>", username);
		Result<Subscription> result = gateway.subscription().cancel(subscription.getId());
		Preconditions.checkArgument(result.isSuccess(), "Couldn't unsubscribe <%s>: %s", username, result.getMessage());
	}

	public boolean update(String username, String email) {
		try {
			return gateway.customer().update(username, new CustomerRequest().email(email)).isSuccess();
		} catch (NotFoundException e) {
			return false;
		} catch (Throwable t) {
			logger.error("Couldn't update email of <{}> to <{}>", username, email, t);
			return false;
		}
	}

	public boolean cancel(String username) {
		try {
			return gateway.customer().delete(username).isSuccess();
		} catch (NotFoundException e) {
			return false;
		}
	}

	public String token(String username) {
		Customer customer = findCustomer(username);
		var request = new ClientTokenRequest();
		if (customer != null) {
			request.customerId(username);
		}
		return gateway.clientToken().generate(request);
	}
}
