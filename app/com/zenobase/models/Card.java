package com.zenobase.models;

import com.braintreegateway.CreditCardRequest;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.DomainNode;
import com.zenobase.json.TokenField;

public class Card extends DomainNode {

	public static final TokenField NUMBER = new TokenField("number");
	public static final TokenField CVV = new TokenField("cvv");
	public static final TokenField EXPIRATION_YEAR = new TokenField("expiration_year");
	public static final TokenField EXPIRATION_MONTH = new TokenField("expiration_month");

	public Card(ObjectNode node) {
		super(node);
	}

	public Card(String number, String cvv, String expirationYear, String expirationMonth) {
		setValue(NUMBER, number);
		setValue(CVV, cvv);
		setValue(EXPIRATION_YEAR, expirationYear);
		setValue(EXPIRATION_MONTH, expirationMonth);
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

	public void fill(CreditCardRequest request) {
		request.number(getNumber()).cvv(getCVV())
			.expirationYear(getExpirationYear())
			.expirationMonth(getExpirationMonth());
	}
}
