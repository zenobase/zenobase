package com.zenobase.auth;

import org.jspecify.annotations.Nullable;

import com.zenobase.oauth.Authorization;

public interface TokenValidator {

	String issuer();

	@Nullable
	Authorization validate(String token);
}
