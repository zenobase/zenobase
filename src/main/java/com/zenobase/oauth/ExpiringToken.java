package com.zenobase.oauth;

import java.io.Serial;
import org.joda.time.DateTime;
import org.jspecify.annotations.Nullable;
import org.scribe.model.Token;

public class ExpiringToken extends Token {

	@Serial
	private static final long serialVersionUID = 1L;

	private final @Nullable DateTime expires;
	private final @Nullable String refreshToken;

	public ExpiringToken(String token, String secret, @Nullable DateTime expires, @Nullable String refreshToken) {
		super(token, secret);
		this.expires = expires;
		this.refreshToken = refreshToken;
	}

	public @Nullable DateTime getExpires() {
		return expires;
	}

	public boolean isExpired() {
		return expires != null && DateTime.now().plusMinutes(1).isAfter(expires);
	}

	public @Nullable String getRefreshToken() {
		return refreshToken;
	}
}
