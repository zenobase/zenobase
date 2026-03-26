package com.zenobase.controllers;

import java.util.Objects;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.commands.DeleteAuthorizationCommand;
import com.zenobase.commands.DeleteBucketCommand;
import com.zenobase.commands.DeleteCredentialsCommand;
import com.zenobase.commands.DeleteTaskCommand;
import com.zenobase.commands.DeleteUserCommand;
import com.zenobase.mail.VerificationMailer;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.models.UserProfile;
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
	public AccountController(
			AuthorizationContext security,
			UserRepository users,
			BucketRepository buckets,
			TaskRepository tasks,
			CredentialsRepository credentials,
			AuthorizationRepository authorizations,
			CommandDispatcher dispatcher,
			VerificationMailer mailer,
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

	public void open(ServerRequest req, ServerResponse res) {
		var form = new SignUpForm(body(req));
		if (!form.valid()) {
			sendBadRequest(res, "invalid request body");
			return;
		}
		if (users.exists(Objects.requireNonNull(form.getUsername()))) {
			sendConflict(res, "user exists");
			return;
		}
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null || auth.getScope() != null) {
			sendUnauthorized(res);
			return;
		}
		var user = new User(auth.getPrincipal().getId(), Objects.requireNonNull(form.getUsername()));
		user.setEmail(Objects.requireNonNull(form.getEmail()));
		user.setHashedPassword(User.hashPassword(Objects.requireNonNull(form.getPassword())));
		user.setSuperuser(users.isEmpty());
		String commandId = dispatcher.dispatch(new CreateUserCommand(auth.getPrincipal(), user));
		mailer.send(user);
		setHeader(res, LOCATION, "/users/" + user.getName());
		setHeader(res, COMMAND_ID, commandId);
		sendCreated(res, new UserProfile(user).toJson());
	}

	public void close(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		User user = new UserLookup(users).getUser(userId);
		if (user == null) {
			sendNotFound(res);
			return;
		}
		if (auth.getScope() != null || !user.is(auth.getPrincipal()) && !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		Command command = buildCloseAccountCommand(auth.getPrincipal(), user, auth);
		payments.cancel(Objects.requireNonNull(user.getName()));
		String commandId = dispatcher.dispatch(command);
		setHeader(res, COMMAND_ID, commandId);
		sendNoContent(res);
	}

	public Command buildCloseAccountCommand(Identity principal, User user, Authorization current) {
		var command = new CompoundCommand(
				principal,
				String.format("closed account %s", user.getName()),
				String.format("reopened account %s", user.getName()));
		command.add(new DeleteUserCommand(principal, user));
		buckets.find(
				new BucketQuery().principalEqualTo(user.asIdentity()).isAlias(true),
				bucket -> command.add(new DeleteBucketCommand(principal, bucket)));
		buckets.find(
				new BucketQuery().principalEqualTo(user.asIdentity()).isAlias(false),
				bucket -> command.add(new DeleteBucketCommand(principal, bucket)));
		tasks.find(
				new TaskQuery().principalEqualTo(user.asIdentity()),
				task -> command.add(new DeleteTaskCommand(principal, task)));
		authorizations.find(new AuthorizationQuery().principalEqualTo(user.asIdentity()), authorization -> {
			if (!current.getId().equals(authorization.getId())) {
				command.add(new DeleteAuthorizationCommand(principal, authorization));
			}
		});
		authorizations.find(
				new AuthorizationQuery().clientEqualTo(user.asIdentity()),
				authorization -> command.add(new DeleteAuthorizationCommand(principal, authorization)));
		credentials.find(
				new CredentialsQuery().principalEqualTo(user.asIdentity()),
				credentials -> command.add(new DeleteCredentialsCommand(principal, credentials)));
		return command;
	}
}
