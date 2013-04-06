package com.zenobase.oauth;

import org.joda.time.DateTime;
import org.scribe.model.Token;

public class RefreshableToken extends Token {

	private static final long serialVersionUID = 1L;

	private final String refreshToken;
	private final DateTime expires;

	public RefreshableToken(String token, String secret, String refreshToken, DateTime expires) {
		super(token, secret);
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
		return expires != null && DateTime.now().isAfter(expires);
	}
}
