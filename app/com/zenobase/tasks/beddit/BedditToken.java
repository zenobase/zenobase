package com.zenobase.tasks.beddit;

import org.scribe.model.Token;
import com.google.common.base.Preconditions;

class BedditToken extends Token {

	private static final long serialVersionUID = 1L;

	private final int userId;

	public BedditToken(String token, int userId) {
		super(token, "");
		Preconditions.checkArgument(userId > 0);
		this.userId = userId;
	}

	public int getUserId() {
		return userId;
	}
}
