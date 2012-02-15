package controllers;

import java.io.IOException;

import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.libs.Crypto;
import play.mvc.Controller;
import play.mvc.Http.StatusCode;

public class AuthenticationController extends Controller {

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
	        session.put("username", username);
			if (remember) {
				response.setCookie("remember", Crypto.sign(username) + "-" + username, "30d");
			}
			response.status = StatusCode.NO_RESPONSE;
		}
	}

	public static void signOut() {
		Logger.info("Signing out %s...", connected());
		session.clear();
		response.removeCookie("remember");
		response.status = StatusCode.NO_RESPONSE;
	}

	public static String connected() {
        return session.get("username");
    }
}
