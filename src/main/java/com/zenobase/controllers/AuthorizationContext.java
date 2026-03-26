package com.zenobase.controllers;

import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.ServerRequest;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import com.zenobase.oauth.Authorization;
import com.zenobase.services.AuthorizationRepository;

public class AuthorizationContext {

	private static final String HEADER_PREFIX = "Bearer ";

	private final AuthorizationRepository authorizations;

	@Inject
	public AuthorizationContext(AuthorizationRepository authorizations) {
		this.authorizations = authorizations;
	}

	public @Nullable Authorization current(ServerRequest request) {
		String token = request.query().first("code").orElse(null);
		if (token == null) {
			token = extractToken(
					request.headers().first(HeaderNames.AUTHORIZATION).orElse(null));
		}
		return token != null ? authorizations.find(token) : null;
	}

	static @Nullable String extractToken(@Nullable String header) {
		return header != null && isOAuthHeader(header) ? header.substring(HEADER_PREFIX.length()) : null;
	}

	private static boolean isOAuthHeader(@Nullable String header) {
		return header != null && header.startsWith(HEADER_PREFIX) && header.length() > HEADER_PREFIX.length();
	}
}
