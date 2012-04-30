package com.zenobase.controllers;

import play.api.libs.Crypto;
import play.mvc.Http;

import com.zenobase.models.Identity;

public class SecurityContext {

	private static final String TOKEN_NAME = "token";
	private static final char TOKEN_SEPARATOR = '-';

	public Identity getPrincipal(boolean createIfNotPresent) {
		Identity principal = getPrincipal();
		if (principal == null && createIfNotPresent) {
			principal = new Identity();
			setPrincipal(principal, true);
		}
		return principal;
	}

	public Identity getPrincipal() {
		Http.Cookie cookie = context().request().cookies().get(TOKEN_NAME);
		if (cookie != null) {
			int p = cookie.value().indexOf(TOKEN_SEPARATOR);
			if (p > 0 && p < cookie.value().length() - 1) {
				String sign = cookie.value().substring(0, p);
				String principal = cookie.value().substring(p + 1);
				if (Crypto.sign(principal).equals(sign)) {
					return new Identity(principal);
				}
			}
			unsetPrincipal();
		}
		return null;
	}

	public void setPrincipal(Identity principal, boolean remember) {
		setPrincipal(TOKEN_NAME, Crypto.sign(principal.getId()) + TOKEN_SEPARATOR + principal.getId(), remember);
	}

	private void setPrincipal(String name, String value, boolean remember) {
		context().response().setCookie(name, value, remember ? 60 * 60 * 24 * 30 : -1, "/", null, false, true);
	}

	public void unsetPrincipal() {
		context().response().discardCookies(TOKEN_NAME);
	}

	private static Http.Context context() {
		return Http.Context.current();
	}
}
