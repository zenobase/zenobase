package controllers;

import models.User;
import play.data.Form;
import play.mvc.Result;
import play.mvc.With;
import services.UserManager;

import com.google.inject.Inject;
import common.Identities;

@With(Timed.class)
public class SecurityController extends ControllerSupport {

	@Inject
	static UserManager users;

	public static Result signIn() {
		Form<SignInForm> form = form(SignInForm.class);
		SignInForm signIn = form.bindFromRequest().get();
		if (form.hasErrors()) {
			return badRequest();
		}
		User user = users.find(signIn.getUsername());
		if (user == null || user.isSuspended() || !user.passwordEquals(signIn.getPassword())) {
			return unauthorized();
		}
		Identities.in(ctx()).set(user.asIdentity(), signIn.isRemember());
		return ok(toJson(user));
	}

	public static Result signOut() {
		Identities.in(ctx()).unset();
		return noContent();
	}
}
