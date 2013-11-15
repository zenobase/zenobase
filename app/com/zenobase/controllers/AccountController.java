package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;

import com.zenobase.commands.CloseAccountCommandBuilder;
import com.zenobase.commands.Command;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.mail.VerificationMailer;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserLookup;
import com.zenobase.services.UserRepository;

public class AccountController extends ControllerSupport {

	private final BucketRepository buckets;
	private final UserRepository users;
	private final CommandDispatcher dispatcher;
	private final VerificationMailer mailer;

	@Inject
	public AccountController(AuthorizationContext security, BucketRepository buckets,
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
		Authorization auth = getCurrentAuthorization();
		if (auth == null || auth.getScope() != null) {
			return unauthorized();
		}
		final User user = new User(auth.getPrincipal().getId(), form.getUsername());
		user.setEmail(form.getEmail());
		user.setHashedPassword(User.hashPassword(form.getPassword()));
		user.setSuperuser(users.isEmpty());
		String commandId = dispatcher.dispatch(new CreateUserCommand(auth.getPrincipal(), user));
		mailer.send(user);
        response().setHeader(LOCATION, com.zenobase.controllers.routes.UserController.get(user.getName()).toString());
		response().setHeader(COMMAND_ID, commandId);
		return created(new UserInfo(user).toJson());
	}

	public Result close(String userId) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
		User user = new UserLookup(users).getUser(userId);
		if (user == null) {
			return notFound();
		}
		if (auth.getScope() != null || !user.is(auth.getPrincipal()) && !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		Command command = new CloseAccountCommandBuilder(auth.getPrincipal(), buckets, user).build();
		String commandId = dispatcher.dispatch(command);
		response().setHeader(COMMAND_ID, commandId);
		return noContent();
	}
}
