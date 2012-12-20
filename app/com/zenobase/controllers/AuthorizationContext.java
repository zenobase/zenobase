package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Http;

import com.zenobase.oauth.Authorization;
import com.zenobase.services.AuthorizationRepository;

public class AuthorizationContext {

	private static final String HEADER_PREFIX = "Bearer ";

	private final AuthorizationRepository authorizations;

	@Inject
	public AuthorizationContext(AuthorizationRepository authorizations) {
		this.authorizations = authorizations;
	}

	public Authorization current() {
		String token = getParameter("code");
		if (token == null) {
			token = extractToken(getHeader(Http.HeaderNames.AUTHORIZATION));
		}
		return token != null ? authorizations.find(token) : null;
	}

	private static String getParameter(String paramName) {
		return Http.Context.current().request().getQueryString(paramName);
	}

	private static String getHeader(String headerName) {
		return Http.Context.current().request().getHeader(headerName);
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
