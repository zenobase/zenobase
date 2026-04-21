package com.zenobase.auth.auth0;

import com.zenobase.auth.TokenValidator;
import com.zenobase.auth.auth0.Auth0TokenValidator.Auth0Claims;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

public class Auth0TokenAuthorizer implements TokenValidator {

	private final Auth0TokenValidator validator;
	private final Auth0UserSynchronizer synchronizer;

	@Inject
	public Auth0TokenAuthorizer(Auth0TokenValidator validator, Auth0UserSynchronizer synchronizer) {
		this.validator = validator;
		this.synchronizer = synchronizer;
	}

	@Override
	public String issuer() {
		return validator.issuer();
	}

	@Override
	public @Nullable Authorization validate(String token) {
		Auth0Claims claims = validator.validate(token);
		if (claims == null) {
			return null;
		}
		Identity identity = synchronizer.sync(claims);
		if (identity == null) {
			return null;
		}
		Authorization authorization = new Authorization(identity);
		if (claims.sessionId() != null) {
			authorization.setSessionId(claims.sessionId());
		}
		return authorization;
	}
}
