package com.zenobase.models;

public enum Role {
	VIEWER(1),
	CONTRIBUTOR(2),
	OWNER(3);

	private final int level;

	Role(int level) {
		this.level = level;
	}

	public boolean implies(Role other) {
		return other.level <= level;
	}
}
