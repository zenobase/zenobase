package com.zenobase.auth;

import com.zenobase.oauth.Authorization;
import org.jspecify.annotations.Nullable;

public interface TokenValidator {
	String issuer();

	@Nullable
	Authorization validate(String token);
}
