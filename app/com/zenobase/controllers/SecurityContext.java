package com.zenobase.controllers;

import play.api.libs.Crypto;
import play.mvc.Http;
import com.google.common.base.Joiner;

import com.zenobase.models.Identity;

public class SecurityContext {

	static final String TOKEN_NAME = "token";
	private static final char TOKEN_SEPARATOR = '-';

	private final byte[] key;

	public SecurityContext(String key) {
		this.key = key.getBytes();
	}

	public Identity getPrincipal(boolean createIfNotPresent) {
		Identity principal = getPrincipal();
		if (principal == null && createIfNotPresent) {
			principal = new Identity();
			setPrincipal(principal, true);
		}
		return principal;
	}

	public Identity getPrincipal() {
		return getPrincipal(context().request().cookies().get(TOKEN_NAME));
	}

	Identity getPrincipal(Http.Cookie cookie) {
		if (cookie != null) {
			int p = cookie.value().indexOf(TOKEN_SEPARATOR);
			if (p > 0 && p < cookie.value().length() - 1) {
				String sign = cookie.value().substring(0, p);
				String principal = cookie.value().substring(p + 1);
				if (sign(principal).equals(sign)) {
					return new Identity(principal);
				}
			}
			unsetPrincipal();
		}
		return null;
	}

	public void setPrincipal(Identity principal, boolean remember) {
		setPrincipal(TOKEN_NAME, Joiner.on(TOKEN_SEPARATOR).join(sign(principal.getId()), principal.getId()), remember);
	}

	private void setPrincipal(String name, String value, boolean remember) {
		context().response().setCookie(name, value, remember ? 60 * 60 * 24 * 30 : -1, "/", null, false, true);
	}

	public void unsetPrincipal() {
		context().response().discardCookies(TOKEN_NAME);
	}

	private String sign(String content) {
		return Crypto.sign(content, key);
	}

	private Http.Context context() {
		return Http.Context.current();
	}
}
