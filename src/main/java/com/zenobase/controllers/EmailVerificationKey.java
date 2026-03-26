package com.zenobase.controllers;

import com.google.common.base.Joiner;
import org.mindrot.jbcrypt.BCrypt;

public class EmailVerificationKey {

	private final String username;
	private final String email;

	public EmailVerificationKey(String username, String email) {
		this.username = username;
		this.email = email;
	}

	public String getKey() {
		return BCrypt.hashpw(concatenate());
	}

	public boolean validate(String key) {
		return key.length() > 50 && BCrypt.checkpw(concatenate(), key);
	}

	private String concatenate() {
		return Joiner.on('\t').join(username, email);
	}
}
