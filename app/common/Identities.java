package common;

import models.Identity;
import play.api.libs.Crypto;
import play.mvc.Http;
import play.mvc.Http.Context;

public class Identities {

	private static final String TOKEN_NAME = "token";
	private static final char TOKEN_SEPARATOR = '-';

	private final Http.Context context;

	private Identities(Context context) {
		this.context = context;
	}

	public static Identities in(Http.Context context) {
		return new Identities(context);
	}

	public boolean is(Identity identity) {
		return identity.equals(get());
	}

	public Identity get(boolean createIfNotPresent) {
		Identity identity = get();
		if (identity == null && createIfNotPresent) {
			identity = new Identity();
			set(identity, true);
		}
		return identity;
	}

	public Identity get() {
		Http.Cookie cookie = context.request().cookies().get(TOKEN_NAME);
		if (cookie != null) {
			int p = cookie.value().indexOf(TOKEN_SEPARATOR);
			if (p > 0 && p < cookie.value().length() - 1) {
				String sign = cookie.value().substring(0, p);
				String identity = cookie.value().substring(p + 1);
				if (Crypto.sign(identity).equals(sign)) {
					return new Identity(identity);
				}
			}
			unset();
		}
		return null;
	}

	public void set(Identity identity, boolean remember) {
		set(TOKEN_NAME, Crypto.sign(identity.getId()) + TOKEN_SEPARATOR + identity.getId(), remember);
	}

	public void unset() {
		context.response().discardCookies(TOKEN_NAME);
	}

	private void set(String name, String value, boolean remember) {
		context.response().setCookie(name, value, remember ? 60 * 60 * 24 * 30 : -1, "/", null, false, true);
	}
}
