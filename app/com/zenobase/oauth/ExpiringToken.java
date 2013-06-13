package com.zenobase.oauth;

import org.joda.time.DateTime;
import org.scribe.model.Token;

public class ExpiringToken extends Token {

	private static final long serialVersionUID = 1L;

	private final DateTime expires;
	private final String refreshToken;

	public ExpiringToken(String token, String secret, DateTime expires, String refreshToken) {
		super(token, secret);
		this.expires = expires;
		this.refreshToken = refreshToken;
	}

	public DateTime getExpires() {
		return expires;
	}

	public boolean isExpired() {
		return expires != null && DateTime.now().plusMinutes(1).isAfter(expires);
	}

	public String getRefreshToken() {
		return refreshToken;
	}
}
