package com.zenobase.tasks.lastfm;

import org.scribe.model.Token;

/**
 * Like a normal token, but need to keep track of the scope (username) temporarily.
 */
public class LastFmToken extends Token {

	private static final long serialVersionUID = 1L;

	private final String scope;

	public LastFmToken(String token, String secret, String scope) {
		super(token, "");
		this.scope = scope;
	}

	public String getScope() {
		return scope;
	}
}
