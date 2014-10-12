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
		return current(Http.Context.current());
	}

	public Authorization current(Http.Context context) {
		String token = getParameter(context, "code");
		if (token == null) {
			token = extractToken(getHeader(context, Http.HeaderNames.AUTHORIZATION));
		}
		return token != null ? authorizations.find(token) : null;
	}

	private static String getParameter(Http.Context context, String paramName) {
		return context.request().getQueryString(paramName);
	}

	private static String getHeader(Http.Context context, String headerName) {
		return context.request().getHeader(headerName);
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
