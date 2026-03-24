package com.zenobase.models;

public enum Role {

	VIEWER,
	CONTRIBUTOR,
	OWNER;

	public boolean implies(Role other) {
		return other.ordinal() <= ordinal();
	}
}
