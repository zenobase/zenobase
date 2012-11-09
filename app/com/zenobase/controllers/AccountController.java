package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.commands.CloseAccountCommandBuilder;
import com.zenobase.commands.Command;
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

	private final BucketRepository buckets;
	private final UserRepository users;
	private final CommandDispatcher dispatcher;
	private final VerificationMailer mailer;

	@Inject
	public AccountController(SecurityContext security, BucketRepository buckets,
		UserRepository users, CommandDispatcher dispatcher, VerificationMailer mailer) {

		super(security);
		this.buckets = buckets;
		this.users = users;
		this.dispatcher = dispatcher;
		this.mailer = mailer;
	}

	@BodyParser.Of(BodyParser.Json.class)
	public Result open() {
		SignUpForm form = new SignUpForm(body());
		if (!form.valid()) {
			return badRequest("invalid request body");
		}
		if (users.exists(form.getUsername())) {
			return conflict("user exists");
		}
		Identity principal = getSecurityContext().getPrincipal(true);
		final User user = new User(principal.getId(), form.getUsername());
		user.setEmail(form.getEmail());
		user.setHashedPassword(User.getHashedPassword(form.getPassword()));
		user.setSuperuser(users.isEmpty());
		dispatcher.dispatch(new CreateUserCommand(principal, user));
		mailer.send(user);
		return created(new UserInfo(user).toJson());
	}

	public Result close(String name) {
		Identity principal = getSecurityContext().getPrincipal();
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
		Command command = new CloseAccountCommandBuilder(principal, buckets, user).build();
		String commandId = dispatcher.dispatch(command);
		return success(commandId);
	}
}
