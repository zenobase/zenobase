package com.zenobase.services;

import java.math.BigDecimal;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.common.collect.Lists;
import play.Play;
import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.CreditCard;
import com.braintreegateway.CreditCardRequest;
import com.braintreegateway.Customer;
import com.braintreegateway.CustomerRequest;
import com.braintreegateway.Environment;
import com.braintreegateway.Plan;
import com.braintreegateway.Result;
import com.braintreegateway.Subscription;
import com.braintreegateway.SubscriptionRequest;
import com.braintreegateway.exceptions.NotFoundException;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

import com.zenobase.models.Card;

public class PaymentGateway {

	private final BraintreeGateway gateway;

	@Inject
	public PaymentGateway(@Named("braintree.merchant_id") String merchantId, @Named("braintree.public_key") String publicKey, @Named("braintree.private_key") String privateKey) {
		this(Play.isProd() ? Environment.PRODUCTION : Environment.SANDBOX, merchantId, publicKey, privateKey);
	}

	PaymentGateway(Environment environment, String merchantId, String publicKey, String privateKey) {
		gateway = new BraintreeGateway(environment, merchantId, publicKey, privateKey);
	}

	public Customer find(String username) {
		try {
			return gateway.customer().find(username);
		} catch (NotFoundException e) {
			return null;
		}
	}

	public void subscribe(String username, String email, Card card, String planId) {
		Customer customer = find(username);
		if (customer != null) {
			replaceSubscription(customer, card, planId);
		} else {
			newSubscription(username, email, card, planId);
		}
	}

	private BigDecimal findPrice(String planId) {
		for (Plan plan : gateway.plan().all()) {
			if (plan.getId().equals(planId)) {
				return plan.getPrice();
			}
		}
		throw new IllegalArgumentException("Couldn't find plan: " + planId);
	}

	private void replaceSubscription(Customer customer, Card card, String planId) {
		Subscription subscription = getSubscription(customer);
		Preconditions.checkArgument(subscription != null, "Expected at least one subscription for <%s>", customer.getId());
		CreditCard creditCard = card != null ? newCreditCard(customer.getId(), card) : getCreditCard(customer);
		Preconditions.checkArgument(creditCard != null, "Expected a card for <%s>", customer.getId());
		SubscriptionRequest request = new SubscriptionRequest().planId(planId).price(findPrice(planId)).paymentMethodToken(creditCard.getToken());
		Result<Subscription> result = gateway.subscription().update(subscription.getId(), request);
		Preconditions.checkArgument(result.isSuccess(), "Couldn't subscribe <%s> to <%s>: %s", customer.getId(), planId, result.getMessage());
	}

	private void newSubscription(String username, String email, Card card, String planId) {
		Preconditions.checkNotNull(card, "Can't create customer <%s> without a card", username);
		Customer customer = newCustomer(username, email, card);
		SubscriptionRequest request = new SubscriptionRequest().planId(planId).paymentMethodToken(getCreditCard(customer).getToken());
		Result<Subscription> result = gateway.subscription().create(request);
		Preconditions.checkArgument(result.isSuccess(), "Couldn't subscribe <%s> to <%s>: %s", username, planId, result.getMessage());
	}

	private Customer newCustomer(String username, String email, Card card) {
		CustomerRequest request = new CustomerRequest().id(username).email(email);
		card.fill(request.creditCard());
		Result<Customer> result = gateway.customer().create(request);
		Preconditions.checkArgument(result.isSuccess(), "Couldn't create customer <%s>: %s", username, result.getMessage());
		return result.getTarget();
	}

	private CreditCard newCreditCard(String username, Card card) {
		CreditCardRequest request = new CreditCardRequest().customerId(username).options().makeDefault(true).done();
		card.fill(request);
		Result<CreditCard> result = gateway.creditCard().create(request);
		Preconditions.checkArgument(result.isSuccess(), "Couldn't store credit card for <%s>: %s", username, result.getMessage());
		return result.getTarget();
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

	static CreditCard getCreditCard(Customer customer) {
		if (customer != null) {
			for (CreditCard card : customer.getCreditCards()) {
				if (card.isDefault() && !card.isExpired()) {
					return card;
				}
			}
		}
		return null;
	}

	public String findPaymentMethodToken(String username) {
		CreditCard card = getCreditCard(find(username));
		return card != null ? card.getLast4() : null;
	}

	public boolean cancel(String username) {
		try {
			return gateway.customer().delete(username).isSuccess();
		} catch (NotFoundException e) {
			return false;
		}
	}
}
