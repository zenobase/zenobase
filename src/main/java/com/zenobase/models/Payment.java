package com.zenobase.models;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.DecimalField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.TokenField;

public class Payment extends DomainNode {

	public static final DecimalField PRICE = new DecimalField("price");
	public static final TokenField NONCE = new TokenField("nonce");

	public Payment(ObjectNode node) {
		super(node);
	}

	public Payment(BigDecimal price) {
		this(price, null);
	}

	public Payment(BigDecimal price, @Nullable String nonce) {
		setValue(PRICE, price);
		setValue(NONCE, nonce);
	}

	public @Nullable BigDecimal getPrice() {
		return getValue(PRICE);
	}

	public @Nullable String getNonce() {
		return getValue(NONCE);
	}
}
