package com.zenobase.services;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

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
import com.braintreegateway.exceptions.NotFoundException;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import play.Logger;
import play.Play;

import com.zenobase.models.Payment;
import com.zenobase.models.Plan;

public class PaymentGateway {

	private final BraintreeGateway gateway;

	@Inject
	public PaymentGateway(@Named("braintree.merchant_id") String merchantId, @Named("braintree.public_key") String publicKey, @Named("braintree.private_key") String privateKey) {
		this(Play.isProd() ? Environment.PRODUCTION : Environment.SANDBOX, merchantId, publicKey, privateKey);
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
		SubscriptionRequest request = new SubscriptionRequest().paymentMethodToken(paymentMethod.getToken());
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
		SubscriptionRequest request = new SubscriptionRequest().planId(plan.getId()).paymentMethodToken(customer.getDefaultPaymentMethod().getToken());
		Result<Subscription> result = gateway.subscription().create(request);
		Preconditions.checkArgument(result.isSuccess(), "Couldn't subscribe <%s> to <%s>: %s", username, plan.getId(), result.getMessage());
	}
	private Customer newCustomer(String username, String email, Payment payment) {
		CustomerRequest request = new CustomerRequest().id(username).email(email).paymentMethodNonce(payment.getNonce());
		Result<Customer> result = gateway.customer().create(request);
		Preconditions.checkArgument(result.isSuccess(), "Couldn't create customer <%s>: %s", username, result.getMessage());
		return result.getTarget();
	}

	private PaymentMethod newPaymentMethod(String username, Payment payment) {
		PaymentMethodRequest request = new PaymentMethodRequest().customerId(username).paymentMethodNonce(payment.getNonce()).options().makeDefault(true).done();
		Result<? extends PaymentMethod> result = gateway.paymentMethod().create(request);
		Preconditions.checkArgument(result.isSuccess(), "Couldn't store credit card for <%s>: %s", username, result.getMessage());
		Preconditions.checkArgument(isVerified(result), "Couldn't verify credit card for <%s>: %s", username, result.getCreditCardVerification().getStatus());
		return result.getTarget();
	}

	private static boolean isVerified(Result<? extends PaymentMethod> result) {
		return result.getCreditCardVerification() != null && result.getCreditCardVerification().getStatus() == CreditCardVerification.Status.VERIFIED;
	}

	static Subscription getSubscription(Customer customer) {
		List<Subscription> subscriptions = Lists.newArrayList();
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
			Logger.error("Couldn't update email of <" + username + "> to <" + email + ">", t);
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
		ClientTokenRequest request = new ClientTokenRequest();
		if (customer != null) {
			request.customerId(username);
		}
		return gateway.clientToken().generate(request);
	}
}
