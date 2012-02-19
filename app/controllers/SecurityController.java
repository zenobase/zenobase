package controllers;

import play.Logger;
import play.api.libs.Crypto;
import play.data.Form;
import play.mvc.Http;
import play.mvc.Result;
import secure.Identity;
import secure.User;
import secure.UserManager;

import com.google.inject.Inject;

public class SecurityController extends ControllerSupport {

	private static final String TOKEN_NAME = "token";
	private static final char TOKEN_SEPARATOR = '-';

	@Inject
	static UserManager users;

	public static Result signUp(SignInForm form) {
		User user = new User(identity(true), form.getUsername());
		user.changePassword(form.getPassword());
		users.store(user);
		setCookie(user.getIdentity(), form.isRemember());
		return noContent();
	}

	public static Result signIn() {
		Form<SignInForm> form = form(SignInForm.class);
		SignInForm signIn = form.bindFromRequest().get();
		if (form.hasErrors()) {
			return badRequest();
		}
		User user = users.find(signIn.getUsername());
		if (user == null) {
			Logger.info(String.format("Signing up %s", signIn.getUsername()));
			return signUp(signIn);
		}
		else if (!user.passwordEquals(signIn.getPassword())) {
			Logger.info(String.format("Rejected sign in for %s", signIn.getUsername()));
			return unauthorized();
		}
		Logger.info(String.format("Signing in %s", signIn.getUsername()));
		setCookie(user.getIdentity(), signIn.isRemember());
		return noContent();
	}

	private static void setCookie(Identity identity, boolean remember) {
		response().setCookie(TOKEN_NAME, Crypto.sign(identity.getId()) + TOKEN_SEPARATOR + identity.getId(), remember ? 60 * 60 * 24 * 30 : -1, "/", null, false, true);
	}

	public static Result signOut() {
		response().discardCookies(TOKEN_NAME);
		return noContent();
	}

	public static User user() {
		Identity identity = identity(false);
		return identity != null ? users.find(identity()) : null;
	}

	public static Identity identity(boolean createIfNotPresent) {
		Identity identity = identity();
		if (identity == null && createIfNotPresent) {
			identity = createIdentity();
		}
		return identity;
	}

	private static Identity identity() {
		Http.Cookie remember = request().cookies().get(TOKEN_NAME);
		if (remember != null && remember.value().indexOf(TOKEN_SEPARATOR) > 0) {
			String sign = remember.value().substring(0, remember.value().indexOf(TOKEN_SEPARATOR));
			String identity = remember.value().substring(remember.value() .indexOf(TOKEN_SEPARATOR) + 1);
			if (Crypto.sign(identity).equals(sign)) {
				Logger.info("Found existing identity: " + identity);
				return new Identity(identity);
			} else {
				Logger.warn("Corrupted identity: " + identity);
			}
		}
		return null;
	}

	private static Identity createIdentity() {
		Identity identity = new Identity();
		Logger.info("Created new identity: " + identity);
		setCookie(identity, true);
		return identity;
	}
}
