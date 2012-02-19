package controllers;

import javax.inject.Inject;

import play.mvc.Result;
import play.mvc.With;
import secure.Identity;
import secure.User;
import secure.UserManager;

@With(Timed.class)
public class UserController extends ControllerSupport {

	@Inject
	static UserManager users;

	public static Result find(String identity) {
		User user = users.find(new Identity(identity));
    	return user != null ? ok(user.toJson()) : noContent();
    }
}
