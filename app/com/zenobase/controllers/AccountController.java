package com.zenobase.controllers;

import javax.inject.Inject;

import play.data.Form;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.commands.CloseAccountCommandBuilder;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.common.SecurityContext;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.services.BucketManager;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.UserManager;

@With(Timed.class)
public class AccountController extends ControllerSupport {

	@Inject
	static BucketManager buckets;

	@Inject
	static UserManager users;

	@Inject
	static CommandQueue queue;

	@Inject
	static VerificationMailer mailer;

	public static Result open() {
		Form<SignUpForm> form = form(SignUpForm.class);
		SignUpForm signUp = form.bindFromRequest().get();
		if (form.hasErrors()) {
			return badRequest();
		}
		if (users.exists(signUp.getUsername())) {
			return badRequest("user exists");
		}
		Identity principal = new SecurityContext(ctx()).getPrincipal(true);
		final User user = new User(principal.getId(), signUp.getUsername());
		user.setEmail(signUp.getEmail());
		user.setHashedPassword(User.getHashedPassword(signUp.getPassword()));
		user.setSuperuser(users.isEmpty());
		queue.dispatch(new CreateUserCommand(principal, user));
		mailer.send(user);
		return created(new UserInfo(user).toJson());
	}

	public static Result close(String name) {
		Identity principal = new SecurityContext(ctx()).getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
		User user = users.find(name);
		if (user == null) {
			return notFound();
		}
		if (!user.equals(principal) && !users.isSuperuser(principal)) {
			return forbidden();
		}
		String commandId = queue.dispatch(new CloseAccountCommandBuilder(principal, buckets, user).build());
        response().setHeader("Undo", String.format("/queue/%s", commandId));
		return noContent();
	}
}
