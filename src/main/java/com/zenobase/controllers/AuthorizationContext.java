package com.zenobase.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.zenobase.auth.TokenValidator;
import com.zenobase.auth.UserStateCache;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.ServerRequest;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationContext {

	private static final Logger logger = LoggerFactory.getLogger(AuthorizationContext.class);

	private static final String HEADER_PREFIX = "Bearer ";

	private final Map<String, TokenValidator> validatorsByIssuer;
	private final UserStateCache userState;

	@Inject
	public AuthorizationContext(Set<TokenValidator> validators, UserStateCache userState) {
		this.validatorsByIssuer = validators.stream().collect(Collectors.toMap(TokenValidator::issuer, v -> v));
		this.userState = userState;
	}

	public UserStateCache.UserState userState(Identity principal) {
		return userState.lookup(principal);
	}

	public @Nullable Authorization current(ServerRequest request) {
		String token = extractToken(request.headers().first(HeaderNames.AUTHORIZATION).orElse(null));
		if (token == null) {
			return null;
		}
		if (!isJwt(token)) {
			logger.debug("Non-JWT token received, rejecting");
			return null;
		}
		Authorization auth = validateJwt(token);
		if (auth == null) {
			return null;
		}
		if (userState.lookup(auth.getPrincipal()) == UserStateCache.UserState.MISSING) {
			logger.debug("Rejecting request: principal {} no longer exists", auth.getPrincipal());
			return null;
		}
		return auth;
	}

	private @Nullable Authorization validateJwt(String token) {
		try {
			DecodedJWT decoded = JWT.decode(token);
			String issuer = decoded.getIssuer();
			TokenValidator validator = validatorsByIssuer.get(issuer);
			if (validator == null) {
				logger.warn("No validator configured for JWT issuer: {}", issuer);
				return null;
			}
			return validator.validate(token);
		} catch (Exception e) {
			logger.warn("JWT decode failed: {}", e.getMessage());
			return null;
		}
	}

	private static boolean isJwt(String token) {
		return token.contains(".");
	}

	static @Nullable String extractToken(@Nullable String header) {
		return header != null && isOAuthHeader(header) ? header.substring(HEADER_PREFIX.length()) : null;
	}

	private static boolean isOAuthHeader(@Nullable String header) {
		return (
			header != null &&
			header.regionMatches(true, 0, HEADER_PREFIX, 0, HEADER_PREFIX.length()) &&
			header.length() > HEADER_PREFIX.length()
		);
	}
}
