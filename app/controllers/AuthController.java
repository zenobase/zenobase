package controllers;

import java.io.IOException;

import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.libs.Crypto;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.StatusCode;

public class AuthController extends Controller {

	private static final String COOKIE_NAME = "token";

	public static void signIn(ObjectNode body) throws IOException {
		Logger.info("Sign in: %s", body);
		String username = body.path("username").asText();
		String password = body.path("password").asText();
		boolean remember = body.path("remember").asBoolean();
		validation.required(username);
		validation.required(password);
		if (validation.hasErrors()) {
			response.status = StatusCode.BAD_REQUEST;
		}
		else {
			response.setCookie(COOKIE_NAME, Crypto.sign(username) + "-" + username, null, "/", remember ? Integer.valueOf(60 * 60 * 24 * 30) : null, false);
			response.status = StatusCode.NO_RESPONSE;
		}
	}

	public static void signOut() {
		Logger.info("Sign out: %s", currentUser());
		session.clear();
		response.removeCookie(COOKIE_NAME);
		response.status = StatusCode.NO_RESPONSE;
	}

	public boolean authenticate(String username, String password) {
		return "123".equals(password);
	}

	public static String currentUser() {
		Http.Cookie remember = request.cookies.get(COOKIE_NAME);
		if (remember != null && remember.value.indexOf('-') > 0) {
			String sign = remember.value.substring(0, remember.value.indexOf('-'));
			String username = remember.value.substring(remember.value .indexOf('-') + 1);
			return Crypto.sign(username).equals(sign) ? username : null;
		}
		return null;
	}
}
