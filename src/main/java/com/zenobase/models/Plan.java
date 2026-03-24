package com.zenobase.models;

import java.math.BigDecimal;

public enum Plan {

	PERSONAL("personal", 3000000, new BigDecimal("5.00"));

	private final String id;
	private final BigDecimal price;
	private final int quota;

	Plan(String id, int quota, BigDecimal price) {
		this.id = id;
		this.quota = quota;
		this.price = price;
	}

	public String getId() {
		return id;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public int getQuota() {
		return quota;
	}

	public static Plan getPlan(BigDecimal price) {
		return price.compareTo(PERSONAL.price) == 0 ? PERSONAL : null;
	}
}
