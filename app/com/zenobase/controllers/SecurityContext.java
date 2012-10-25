package com.zenobase.controllers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import play.api.libs.Crypto;
import play.mvc.Http;
import com.google.common.base.Joiner;

import com.zenobase.models.Identity;

public class SecurityContext {

	static final String TOKEN_NAME = "token";
	private static final char TOKEN_SEPARATOR = '-';
	private static final Pattern AUTHORIZATION_PATTERN = Pattern.compile("zeno id=\"([^\"]+)\", hash=\"([^\"]+)\"");

	private final byte[] key;

	@Inject
	public SecurityContext(@Named("application.secret") String key) {
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
		String header = context().request().getHeader("Authorization");
		return header != null ?
			getPrincipal(header) :
			getPrincipal(context().request().cookies().get(TOKEN_NAME));
	}

	Identity getPrincipal(Http.Cookie cookie) {
		if (cookie != null) {
			int p = cookie.value().indexOf(TOKEN_SEPARATOR);
			if (p > 0 && p < cookie.value().length() - 1) {
				String id = cookie.value().substring(p + 1);
				String hash = cookie.value().substring(0, p);
				if (sign(id).equals(hash)) {
					return new Identity(id);
				}
			}
			unsetPrincipal();
		}
		return null;
	}

	Identity getPrincipal(String header) {
		if (header != null) {
			Matcher m = AUTHORIZATION_PATTERN.matcher(header);
			if (m.matches()) {
				String id = m.group(1);
				String hash = m.group(2);
				if (sign(id).equals(hash)) {
					return new Identity(id);
				}
			}
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

	public String sign(String content) {
		return Crypto.sign(content, key);
	}

	private Http.Context context() {
		return Http.Context.current();
	}
}
