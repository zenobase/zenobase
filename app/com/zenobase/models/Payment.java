package com.zenobase.models;

import java.math.BigDecimal;

import com.braintreegateway.CreditCardRequest;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.json.BooleanField;
import com.zenobase.json.DecimalField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.TokenField;

public class Payment extends DomainNode {

	public static final DecimalField PRICE = new DecimalField("price");
	public static final TokenField NUMBER = new TokenField("number");
	public static final TokenField CVV = new TokenField("cvv");
	public static final TokenField EXPIRATION_YEAR = new TokenField("expiration_year");
	public static final TokenField EXPIRATION_MONTH = new TokenField("expiration_month");
	public static final BooleanField PAST_DUE = new BooleanField("past_due");

	public Payment(ObjectNode node) {
		super(node);
	}

	public Payment(BigDecimal price) {
		this(price, null, null, null, null, null);
	}

	public Payment(BigDecimal price, String number, String cvv, String expirationYear, String expirationMonth) {
		this(price, number, cvv, expirationYear, expirationMonth, null);
	}

	public Payment(BigDecimal price, String number, String cvv, String expirationYear, String expirationMonth, Boolean pastDue) {
		setValue(PRICE, price);
		setValue(NUMBER, number);
		setValue(CVV, cvv);
		setValue(EXPIRATION_YEAR, expirationYear);
		setValue(EXPIRATION_MONTH, expirationMonth);
		setValue(PAST_DUE, pastDue);
	}

	public BigDecimal getPrice() {
		return getValue(PRICE);
	}

	public String getNumber() {
		return getValue(NUMBER);
	}

	public String getCVV() {
		return getValue(CVV);
	}

	public String getExpirationYear() {
		return getValue(EXPIRATION_YEAR);
	}

	public String getExpirationMonth() {
		return getValue(EXPIRATION_MONTH);
	}

	public boolean hasCreditCard() {
		return getNumber() != null;
	}

	public boolean isPastDue() {
		return Objects.firstNonNull(getValue(PAST_DUE), Boolean.FALSE);
	}

	public void fill(CreditCardRequest request) {
		request.number(getNumber()).cvv(getCVV())
			.expirationYear(getExpirationYear())
			.expirationMonth(getExpirationMonth());
	}
}
