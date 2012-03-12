package controllers;

import play.data.Form;
import play.mvc.Result;
import play.mvc.With;
import secure.IdentityHelper;
import secure.User;
import secure.UserManager;

import com.google.inject.Inject;

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
		if (user == null || !user.passwordEquals(signIn.getPassword())) {
			return unauthorized();
		}
		IdentityHelper.in(ctx()).set(user.getIdentity(), signIn.isRemember());
		return ok(user.toJson());
	}

	public static Result signOut() {
		IdentityHelper.in(ctx()).unset();
		return noContent();
	}
}
