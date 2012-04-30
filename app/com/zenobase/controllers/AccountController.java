package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.commands.CloseAccountCommandBuilder;
import com.zenobase.commands.CreateUserCommand;
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

	@BodyParser.Of(BodyParser.Json.class)
	public static Result open() {
		SignUpForm form = new SignUpForm(body());
		if (!form.valid()) {
			return badRequest("invalid request body");
		}
		if ("guest".equals(form.getUsername()) || users.exists(form.getUsername())) {
			return status(CONFLICT, "user exists");
		}
		Identity principal = auth.getPrincipal(true);
		final User user = new User(principal.getId(), form.getUsername());
		user.setEmail(form.getEmail());
		user.setHashedPassword(User.getHashedPassword(form.getPassword()));
		user.setSuperuser(users.isEmpty());
		queue.dispatch(new CreateUserCommand(principal, user));
		mailer.send(user);
		return created(new UserInfo(user).toJson());
	}

	public static Result close(String name) {
		Identity principal = auth.getPrincipal();
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
		return ok(receipt(commandId));
	}
}
