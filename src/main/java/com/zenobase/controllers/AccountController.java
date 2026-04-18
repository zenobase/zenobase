package com.zenobase.controllers;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;

import com.zenobase.auth.UserDirectory;
import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.DeleteBucketCommand;
import com.zenobase.commands.DeleteCredentialsCommand;
import com.zenobase.commands.DeleteTaskCommand;
import com.zenobase.commands.DeleteUserCommand;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.BucketQuery;
import com.zenobase.queries.CredentialsQuery;
import com.zenobase.queries.TaskQuery;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.repositories.TaskRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserLookup;

public class AccountController extends ControllerSupport {

	private final UserRepository users;
	private final BucketRepository buckets;
	private final TaskRepository tasks;
	private final CredentialsRepository credentials;
	private final CommandDispatcher dispatcher;
	private final UserDirectory userDirectory;

	@Inject
	public AccountController(
		AuthorizationContext security,
		UserRepository users,
		BucketRepository buckets,
		TaskRepository tasks,
		CredentialsRepository credentials,
		CommandDispatcher dispatcher,
		UserDirectory userDirectory
	) {
		super(security);
		this.users = users;
		this.buckets = buckets;
		this.tasks = tasks;
		this.credentials = credentials;
		this.dispatcher = dispatcher;
		this.userDirectory = userDirectory;
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
		if (auth.getScope() != null || (!user.is(auth.getPrincipal()) && !users.isSuperuser(auth.getPrincipal()))) {
			sendForbidden(res);
			return;
		}
		Command command = buildCloseAccountCommand(auth.getPrincipal(), user, auth);
		String commandId = dispatcher.dispatch(command);
		userDirectory.deleteUser(user);
		setHeader(res, COMMAND_ID, commandId);
		sendNoContent(res);
	}

	public Command buildCloseAccountCommand(Identity principal, User user, Authorization current) {
		var command = new CompoundCommand(
			principal,
			String.format("closed account %s", user.getName()),
			String.format("reopened account %s", user.getName())
		);
		command.add(new DeleteUserCommand(principal, user));
		buckets.find(new BucketQuery().principalEqualTo(user.asIdentity()).isAlias(true), bucket ->
			command.add(new DeleteBucketCommand(principal, bucket))
		);
		buckets.find(new BucketQuery().principalEqualTo(user.asIdentity()).isAlias(false), bucket ->
			command.add(new DeleteBucketCommand(principal, bucket))
		);
		tasks.find(new TaskQuery().principalEqualTo(user.asIdentity()), task ->
			command.add(new DeleteTaskCommand(principal, task))
		);
		credentials.find(new CredentialsQuery().principalEqualTo(user.asIdentity()), credentials ->
			command.add(new DeleteCredentialsCommand(principal, credentials))
		);
		return command;
	}
}
