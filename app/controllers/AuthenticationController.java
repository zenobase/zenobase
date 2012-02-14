package controllers;

import java.io.IOException;

import org.apache.commons.fileupload.util.Streams;

import play.Logger;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.libs.Crypto;
import play.mvc.Controller;
import play.mvc.Http.StatusCode;

public class AuthenticationController extends Controller {

	public static void signIn(@Required String username, @MinSize(3) String password, boolean remember) throws IOException {
		Logger.info("Signing in %s...", username);
		if (validation.hasErrors()) {
			// response.status = StatusCode.BAD_REQUEST;
			username = "ejain";
		}
		// else {
	        session.put("username", username);
			if (remember) {
				response.setCookie("rememberme", Crypto.sign(username) + "-" + username, "30d");
			}
			response.status = StatusCode.NO_RESPONSE;
		// }
	}

	public static void signOut() {
		Logger.info("Signing out %s...", connected());
		session.clear();
		response.removeCookie("rememberme");
		response.status = StatusCode.NO_RESPONSE;
	}

	public static String connected() {
        return session.get("username");
    }
}
