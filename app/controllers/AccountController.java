package controllers;

import javax.inject.Inject;

import models.User;

import play.data.Form;
import play.mvc.Result;
import play.mvc.With;
import models.Identity;
import services.BucketManager;
import services.CommandQueue;
import services.UserManager;

import commands.CloseAccountCommandBuilder;
import commands.CreateUserCommand;
import common.Identities;

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
		if (users.exists(signUp.getUsername())) {
			return badRequest("user exists");
		}
		Identity identity = Identities.in(ctx()).get(true);
		User user = new User(identity.getId(), signUp.getUsername());
		user.setEmail(signUp.getEmail());
		user.changePassword(signUp.getPassword());
		user.setSuperuser(users.isEmpty());
		queue.dispatch(new CreateUserCommand(identity, user));
		return created(toJson(user));
	}

	public static Result close(String name) {
		Identity identity = Identities.in(ctx()).get();
		if (identity == null) {
			return unauthorized();
		}
		User user = users.find(name);
		if (user == null) {
			return notFound();
		}
		if (!user.equals(identity) && !users.isSuperuser(identity)) {
			return forbidden();
		}
		String commandId = queue.dispatch(new CloseAccountCommandBuilder(identity, buckets, user).build());
        response().setHeader("Undo", String.format("/queue/%s", commandId));
		return noContent();
	}
}
