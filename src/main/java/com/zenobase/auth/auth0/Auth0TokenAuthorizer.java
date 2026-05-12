package com.zenobase.auth.auth0;

import com.zenobase.auth.TokenValidator;
import com.zenobase.auth.auth0.Auth0TokenValidator.Auth0Claims;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

public class Auth0TokenAuthorizer implements TokenValidator {

	/**
	 * Sentinel scope carried on Authorization objects produced from tokens whose audience matches the
	 * {@code auth0.external_audience} (e.g. MCP clients, third-party REST integrations). First-party tokens have
	 * scope {@code null}. Existing call sites use {@code auth.getScope() != null} to distinguish first-party from
	 * non-first-party requests; that pattern continues to work unchanged.
	 */
	public static final String EXTERNAL_SCOPE = "external";

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
		Identity client = claims.clientId() != null ? new Identity(claims.clientId()) : null;
		String scope = scopeFor(claims.audience());
		return new Authorization(identity, client, scope);
	}

	private @Nullable String scopeFor(@Nullable String audience) {
		return scopeFor(audience, validator.externalAudience());
	}

	/**
	 * Pure function — exposed for testing. Returns {@link #EXTERNAL_SCOPE} when {@code audience} matches the
	 * configured external audience, {@code null} otherwise (= first-party).
	 */
	static @Nullable String scopeFor(@Nullable String audience, @Nullable String externalAudience) {
		if (externalAudience != null && externalAudience.equals(audience)) {
			return EXTERNAL_SCOPE;
		}
		return null;
	}
}
