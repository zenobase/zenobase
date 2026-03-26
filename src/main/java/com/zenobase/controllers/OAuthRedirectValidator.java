package com.zenobase.controllers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import com.zenobase.models.User;

class OAuthRedirectValidator {

	private final User client;

	public OAuthRedirectValidator(User client) {
		this.client = client;
	}

	public boolean valid(String uri) {
		try {
			return valid(new URI(uri));
		} catch (URISyntaxException e) {
			return false;
		}
	}

	private boolean valid(URI uri) {
		return isCustomScheme(uri) || isLocalhost(uri) || sameDomain(Objects.requireNonNull(client.getEmail()), uri);
	}

	private static boolean isCustomScheme(URI uri) {
		return uri.getScheme() != null && uri.getScheme().startsWith("x");
	}

	private static boolean isLocalhost(URI uri) {
		return "localhost".equals(uri.getHost());
	}

	private static boolean sameDomain(String email, URI uri) {
		String domain = email.substring(email.indexOf('@') + 1);
		return uri.getHost() != null
				&& (uri.getHost().equals(domain) || uri.getHost().endsWith('.' + domain));
	}
}
