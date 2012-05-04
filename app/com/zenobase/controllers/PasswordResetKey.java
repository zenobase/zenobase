package com.zenobase.controllers;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import com.zenobase.common.BCrypt;
import com.zenobase.models.User;

public class PasswordResetKey {

	private final User user;
	private final DateTime expires;

	public PasswordResetKey(User user) {
		this(user, new DateTime(DateTimeZone.UTC).plusDays(1));
	}

	public PasswordResetKey(User user, String expirationToken) {
		this(user, parse(expirationToken));
	}

	PasswordResetKey(User user, DateTime expires) {
		Preconditions.checkNotNull(user.getName());
		Preconditions.checkNotNull(user.getHashedPassword());
		this.user = user;
		this.expires = expires;
	}

	public String getKey() {
		return BCrypt.hashpw(concatenate());
	}

	public String getExpirationToken() {
		return toString(expires);
	}

	public boolean validate(String key) {
		return key.length() > 50 &&
			expires.isAfter(System.currentTimeMillis()) &&
			BCrypt.checkpw(concatenate(), key);
	}

	private String concatenate() {
		return Joiner.on('\t').join(user.getName(), user.getHashedPassword(), toString(expires));
	}

	private static DateTime parse(String value) {
		return new DateTime(Long.parseLong(value, 36), DateTimeZone.UTC);
	}

	private static String toString(DateTime time) {
		return Long.toString(time.getMillis(), 36);
	}
}
