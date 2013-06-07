package com.zenobase.oauth;

import org.joda.time.DateTime;
import org.scribe.model.Token;

public class OAuth2Token extends Token {

	private static final long serialVersionUID = 1L;

	private final String refreshToken;
	private final DateTime expires;

	public OAuth2Token(String token, String refreshToken, DateTime expires) {
		super(token, "");
		this.refreshToken = refreshToken;
		this.expires = expires;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public DateTime getExpires() {
		return expires;
	}

	public boolean isExpired() {
		return expires != null && DateTime.now().isAfter(expires); // TODO add a few minutes, to be safe?
	}
}
