package controllers;

import java.io.IOException;

import play.api.libs.Crypto;
import play.data.Form;
import play.mvc.Http;
import play.mvc.Result;

public class SecurityController extends ControllerSupport {

	private static final String COOKIE_NAME = "token";

	public static Result signIn() throws IOException {
		Form<SignInForm> form = form(SignInForm.class);
		SignInForm signIn = form.bindFromRequest().get();
		if (form.hasErrors()) {
			return badRequest();
		}
		else {
			response().setCookie(COOKIE_NAME, Crypto.sign(signIn.getUsername()) + "-" + signIn.getUsername(), signIn.isRemember() ? 60 * 60 * 24 * 30 : -1);
			return noContent();
		}
	}

	public static Result signOut() {
		response().discardCookies(COOKIE_NAME);
		return noContent();
	}

	public boolean authenticate(SignInForm sigIn) {
		return "123".equals(sigIn.getPassword());
	}

	public static String user() {
		Http.Cookie remember = request().cookies().get(COOKIE_NAME);
		if (remember != null && remember.value().indexOf('-') > 0) {
			String sign = remember.value().substring(0, remember.value().indexOf('-'));
			String username = remember.value().substring(remember.value() .indexOf('-') + 1);
			return Crypto.sign(username).equals(sign) ? username : null;
		}
		return null;
	}
}
