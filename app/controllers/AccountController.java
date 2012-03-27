package controllers;

import javax.inject.Inject;

import play.data.Form;
import play.mvc.Result;
import play.mvc.With;
import secure.Identity;
import secure.IdentityHelper;
import secure.User;
import secure.UserManager;
import services.BucketManager;
import services.CommandQueue;

import commands.CloseAccountCommand;
import commands.CreateUserCommand;

@With(Timed.class)
public class AccountController extends ControllerSupport {

	@Inject
	static BucketManager buckets;

	@Inject
	static UserManager users;

	@Inject
	static CommandQueue queue;

	public static Result open() {
		Form<SignUpForm> form = form(SignUpForm.class);
		SignUpForm signUp = form.bindFromRequest().get();
		if (form.hasErrors()) {
			return badRequest();
		}
		User user = users.find(signUp.getUsername());
		if (user != null) {
			return badRequest("user exists");
		}
		Identity identity = IdentityHelper.in(ctx()).get(true);
		user = new User(identity, signUp.getUsername());
		user.setEmail(signUp.getEmail());
		user.changePassword(signUp.getPassword());
		user.setSuperuser(users.isEmpty());
		queue.execute(new CreateUserCommand(users, user));
		return created(user.toJson());
	}

	public static Result close(String name) {
		Identity identity = IdentityHelper.in(ctx()).get();
		if (identity == null) {
			return unauthorized();
		}
		User user = users.find(name);
		if (user == null) {
			return notFound();
		}
		if (!user.getIdentity().equals(identity)) {
			return forbidden();
		}
		queue.execute(new CloseAccountCommand(buckets, users, user));
		IdentityHelper.in(ctx()).unset();
		return noContent();
	}
}
