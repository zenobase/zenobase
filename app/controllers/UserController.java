package controllers;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;

public class UserController extends Controller {

	@Before
    public static void get() {
    	if (Security.isConnected()) {
    		Logger.info("User: %s", Security.connected());
    		renderArgs.put("user", Security.connected());
    	}
    }
}
