package com.zenobase.controllers;

import jakarta.inject.Inject;

import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.ServerRequest;

import com.zenobase.oauth.Authorization;
import com.zenobase.services.AuthorizationRepository;

public class AuthorizationContext {

	private static final String HEADER_PREFIX = "Bearer ";

	private final AuthorizationRepository authorizations;

	@Inject
	public AuthorizationContext(AuthorizationRepository authorizations) {
		this.authorizations = authorizations;
	}

	public Authorization current(ServerRequest request) {
		String token = request.query().first("code").orElse(null);
		if (token == null) {
			token = extractToken(request.headers().first(HeaderNames.AUTHORIZATION).orElse(null));
		}
		return token != null ? authorizations.find(token) : null;
	}

	static String extractToken(String header) {
		return isOAuthHeader(header) ? header.substring(HEADER_PREFIX.length()) : null;
	}

	private static boolean isOAuthHeader(String header) {
		return header != null
			&& header.startsWith(HEADER_PREFIX)
			&& header.length() > HEADER_PREFIX.length();
	}
}
