package com.zenobase.controllers;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import com.zenobase.auth.UserDirectory;
import com.zenobase.commands.ChangeQuotaCommand;
import com.zenobase.commands.ChangeUserEmailCommand;
import com.zenobase.commands.Command;
import com.zenobase.commands.OptInCommand;
import com.zenobase.commands.OptOutCommand;
import com.zenobase.commands.SuspendUserCommand;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.models.UserProfile;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserLookup;

public class UserController extends ControllerSupport {

	private final UserRepository users;
	private final CommandDispatcher dispatcher;
	private final UserDirectory userDirectory;

	@Inject
	public UserController(
		AuthorizationContext security,
		UserRepository users,
		CommandDispatcher dispatcher,
		UserDirectory userDirectory
	) {
		super(security);
		this.users = users;
		this.dispatcher = dispatcher;
		this.userDirectory = userDirectory;
	}

	public void get(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		Authorization auth = getCurrentAuthorization(req);
		User user = new UserLookup(users).getUser(userId);
		if (user == null) {
			sendNotFound(res);
			return;
		}
		sendOk(res, toJson(user, auth));
	}

	private ObjectNode toJson(User user, @Nullable Authorization auth) {
		return (
				auth != null &&
				((auth.getScope() == null && user.is(auth.getPrincipal())) || users.isSuperuser(auth.getPrincipal()))
			)
			? new UserProfile(user).toJson()
			: new UserInfo(user).toJson();
	}

	public void update(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		ObjectNode body = body(req);
		User user = new UserLookup(users).getUser(userId);
		if (user == null) {
			sendNotFound(res, "user not found");
			return;
		}
		UpdateUserForm form = new UpdateUserForm(body);
		if (form.getEmail() != null) {
			updateEmail(req, res, form, user);
			return;
		}
		if (form.hasQuota()) {
			updateQuota(req, res, form, user);
			return;
		}
		if (form.isSuspended() != null) {
			updateSuspension(req, res, user, form.isSuspended());
			return;
		}
		if (form.isOptedOut() != null) {
			updateOptedOut(req, res, form, user);
			return;
		}
		sendBadRequest(res, "invalid update request");
	}

	private void updateEmail(ServerRequest req, ServerResponse res, UpdateUserForm form, User user) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null || (!user.is(auth.getPrincipal()) && !users.isSuperuser(auth.getPrincipal()))) {
			sendForbidden(res);
			return;
		}
		String email = form.getEmail();
		if (email == null || email.isEmpty()) {
			sendBadRequest(res, "invalid email address");
			return;
		}
		String userName = Objects.requireNonNull(user.getName());
		String userEmail = Objects.requireNonNull(user.getEmail());
		String commandId = dispatcher.dispatch(
			new ChangeUserEmailCommand(
				auth.getPrincipal(),
				userName,
				userEmail,
				Objects.requireNonNull(email),
				user.isVerified(),
				false
			)
		);
		userDirectory.updateEmail(user, email);
		setHeader(res, COMMAND_ID, commandId);
		sendNoContent(res);
	}

	private void updateSuspension(ServerRequest req, ServerResponse res, User user, boolean suspended) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null || !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		Command command = new SuspendUserCommand(
			auth.getPrincipal(),
			Objects.requireNonNull(user.getName()),
			suspended
		);
		String commandId = dispatcher.dispatch(command);
		setHeader(res, COMMAND_ID, commandId);
		sendNoContent(res);
	}

	private void updateQuota(ServerRequest req, ServerResponse res, UpdateUserForm form, User user) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null || !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		String commandId = dispatcher.dispatch(
			new ChangeQuotaCommand(auth.getPrincipal(), user.getName(), user.getQuota(), form.getQuota())
		);
		setHeader(res, COMMAND_ID, commandId);
		sendNoContent(res);
	}

	private void updateOptedOut(ServerRequest req, ServerResponse res, UpdateUserForm form, User user) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null || (!user.is(auth.getPrincipal()) && !users.isSuperuser(auth.getPrincipal()))) {
			sendForbidden(res);
			return;
		}
		Command c = Objects.requireNonNull(form.isOptedOut())
			? new OptOutCommand(auth.getPrincipal(), Objects.requireNonNull(user.getName()))
			: new OptInCommand(auth.getPrincipal(), Objects.requireNonNull(user.getName()));
		String commandId = dispatcher.dispatch(c);
		setHeader(res, COMMAND_ID, commandId);
		sendNoContent(res);
	}
}
