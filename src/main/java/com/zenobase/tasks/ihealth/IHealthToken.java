package com.zenobase.tasks.ihealth;

import com.google.common.base.Preconditions;
import org.joda.time.DateTime;

import com.zenobase.oauth.ExpiringToken;

class IHealthToken extends ExpiringToken {

	private static final long serialVersionUID = 1L;

	private final String userId;

	public IHealthToken(String token, String secret, DateTime expires, String refreshToken, String userId) {
		super(token, secret, expires, refreshToken);
		this.userId = Preconditions.checkNotNull(userId);
	}

	public String getUserId() {
		return userId;
	}
}
