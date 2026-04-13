package com.zenobase.controllers;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.ServerRequest;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.auth.TokenValidator;
import com.zenobase.oauth.Authorization;

public class AuthorizationContext {

	private static final Logger logger = LoggerFactory.getLogger(AuthorizationContext.class);

	private static final String HEADER_PREFIX = "Bearer ";

	private final Map<String, TokenValidator> validatorsByIssuer;

	@Inject
	public AuthorizationContext(Set<TokenValidator> validators) {
		this.validatorsByIssuer = validators.stream().collect(Collectors.toMap(TokenValidator::issuer, v -> v));
	}

	public @Nullable Authorization current(ServerRequest request) {
		String token =
				extractToken(request.headers().first(HeaderNames.AUTHORIZATION).orElse(null));
		if (token == null) {
			return null;
		}
		if (!isJwt(token)) {
			logger.debug("Non-JWT token received, rejecting");
			return null;
		}
		return validateJwt(token);
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
		return header != null && header.startsWith(HEADER_PREFIX) && header.length() > HEADER_PREFIX.length();
	}
}
