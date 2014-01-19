package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.commands.DeleteAuthorizationCommand;
import com.zenobase.commands.DeleteBucketCommand;
import com.zenobase.commands.DeleteCredentialsCommand;
import com.zenobase.commands.DeleteTaskCommand;
import com.zenobase.commands.DeleteUserCommand;
import com.zenobase.common.Callback;
import com.zenobase.mail.VerificationMailer;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.AuthorizationQuery;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.BucketQuery;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CredentialsQuery;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.services.PaymentGateway;
import com.zenobase.services.TaskQuery;
import com.zenobase.services.TaskRepository;
import com.zenobase.services.UserLookup;
import com.zenobase.services.UserRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.Task;

public class AccountController extends ControllerSupport {

	private final UserRepository users;
	private final BucketRepository buckets;
	private final TaskRepository tasks;
	private final CredentialsRepository credentials;
	private final AuthorizationRepository authorizations;
	private final CommandDispatcher dispatcher;
	private final VerificationMailer mailer;
	private final PaymentGateway payments;

	@Inject
	public AccountController(AuthorizationContext security, UserRepository users,
		BucketRepository buckets, TaskRepository tasks, CredentialsRepository credentials,
		AuthorizationRepository authorizations, CommandDispatcher dispatcher, VerificationMailer mailer,
		PaymentGateway payments) {

		super(security);
		this.users = users;
		this.buckets = buckets;
		this.tasks = tasks;
		this.credentials = credentials;
		this.authorizations = authorizations;
		this.dispatcher = dispatcher;
		this.mailer = mailer;
		this.payments = payments;
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
		Command command = buildCloseAccountCommand(auth.getPrincipal(), user, auth);
		payments.cancel(user.getName());
		String commandId = dispatcher.dispatch(command);
		response().setHeader(COMMAND_ID, commandId);
		return noContent();
	}

	public Command buildCloseAccountCommand(final Identity principal, User user, final Authorization current) {
		final CompoundCommand command = new CompoundCommand(principal, String.format("closed account %s", user.getName()), String.format("reopened account %s", user.getName()));
		command.add(new DeleteUserCommand(principal, user));
		buckets.find(new BucketQuery().principalEqualTo(user.asIdentity()).isAlias(true), new Callback<Bucket>() {
			@Override
			public void call(Bucket bucket) {
				command.add(new DeleteBucketCommand(principal, bucket));
			}
		});
		buckets.find(new BucketQuery().principalEqualTo(user.asIdentity()).isAlias(false), new Callback<Bucket>() {
			@Override
			public void call(Bucket bucket) {
				command.add(new DeleteBucketCommand(principal, bucket));
			}
		});
    	tasks.find(new TaskQuery().principalEqualTo(user.asIdentity()), new Callback<Task>() {
    		@Override
    		public void call(Task task) {
    			command.add(new DeleteTaskCommand(principal, task));
    		}
		});
    	authorizations.find(new AuthorizationQuery().principalEqualTo(user.asIdentity()), new Callback<Authorization>() {
			@Override
			public void call(Authorization authorization) {
				if (!current.getId().equals(authorization.getId())) {
					command.add(new DeleteAuthorizationCommand(principal, authorization));
				}
			}
		});
    	authorizations.find(new AuthorizationQuery().clientEqualTo(user.asIdentity()), new Callback<Authorization>() {
			@Override
			public void call(Authorization authorization) {
				command.add(new DeleteAuthorizationCommand(principal, authorization));
			}
		});
    	credentials.find(new CredentialsQuery().principalEqualTo(user.asIdentity()), new Callback<Credentials>() {
    		@Override
    		public void call(Credentials credentials) {
    			command.add(new DeleteCredentialsCommand(principal, credentials));
    		}
		});
		return command;
	}
}
