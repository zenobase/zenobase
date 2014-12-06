package com.zenobase.tasks.trackthisforme;

public class Category {

	private final String id;
	private final String name;
	private final String symbol;

	public Category(String id, String name, String symbol) {
		this.id = id;
		this.name = name;
		this.symbol = symbol;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getSymbol() {
		return symbol;
	}
}
