package com.zenobase.controllers;

import java.net.URI;

import com.zenobase.models.User;

class OAuthRedirectValidator {

	private final User client;

	public OAuthRedirectValidator(User client) {
		this.client = client;
	}

	public boolean valid(String uri) {
		try {
			return valid(URI.create(uri));
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public boolean valid(URI uri) {
		return isCustomScheme(uri) || sameDomain(client.getEmail(), uri);
	}

	private static boolean isCustomScheme(URI uri) {
    	return uri.getScheme().startsWith("x-");
	}

	private static boolean sameDomain(String email, URI uri) {
		String domain = email.substring(email.indexOf('@') + 1);
    	return uri.getHost().equals(domain) || uri.getHost().endsWith('.' + domain);
	}
}
