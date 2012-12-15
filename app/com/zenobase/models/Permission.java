package com.zenobase.models;

public enum Permission {

	NONE,
	USE,
	CONTRIBUTE,
	ALL;

	public boolean implies(Permission other) {
		return other.ordinal() <= ordinal();
	}
}
