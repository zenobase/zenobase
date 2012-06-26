package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.commands.CloseAccountCommandBuilder;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.mail.VerificationMailer;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

@With(Timed.class)
public class AccountController extends ControllerSupport {

	@Inject
	static BucketRepository buckets;

	@Inject
	static UserRepository users;

	@Inject
	static CommandDispatcher dispatcher;

	@Inject
	static VerificationMailer mailer;

	@BodyParser.Of(BodyParser.Json.class)
	public static Result open() {
		SignUpForm form = new SignUpForm(body());
		if (!form.valid()) {
			return badRequest("invalid request body");
		}
		if (users.exists(form.getUsername())) {
			return status(CONFLICT, "user exists");
		}
		Identity principal = auth.getPrincipal(true);
		final User user = new User(principal.getId(), form.getUsername());
		user.setEmail(form.getEmail());
		user.setHashedPassword(User.getHashedPassword(form.getPassword()));
		user.setSuperuser(users.isEmpty());
		dispatcher.dispatch(new CreateUserCommand(principal, user));
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
		if (!user.is(principal) && !users.isSuperuser(principal)) {
			return forbidden();
		}
		String commandId = dispatcher.dispatch(new CloseAccountCommandBuilder(principal, buckets, user).build());
		return ok(receipt(commandId));
	}
}
