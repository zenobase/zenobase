package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.commands.ChangeQuotaCommand;
import com.zenobase.commands.ChangeUserEmailCommand;
import com.zenobase.commands.ChangeUserPasswordCommand;
import com.zenobase.commands.ChangeUserVerifiedCommand;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateAuthorizationCommand;
import com.zenobase.commands.DeleteAuthorizationCommand;
import com.zenobase.common.Callback;
import com.zenobase.json.Nodes;
import com.zenobase.mail.VerificationMailer;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.models.UserProfile;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.AuthorizationQuery;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserLookup;
import com.zenobase.services.UserRepository;

public class UserController extends ControllerSupport {

	private final UserRepository users;
	private final AuthorizationRepository authorizations;
	private final CommandDispatcher dispatcher;
	private final VerificationMailer mailer;

	@Inject
	public UserController(AuthorizationContext security, UserRepository users,
		AuthorizationRepository authorizations, CommandDispatcher dispatcher,
		VerificationMailer mailer) {

		super(security);
		this.users = users;
		this.authorizations = authorizations;
		this.dispatcher = dispatcher;
		this.mailer = mailer;
	}

	public Result get(String userId) {
		Authorization auth = getCurrentAuthorization();
		User user = new UserLookup(users).getUser(userId);
		if (user == null) {
			return notFound();
		}
		return ok(toJson(user, auth));
	}

	private ObjectNode toJson(User user, Authorization auth) {
		return auth != null && (auth.getScope() == null && user.is(auth.getPrincipal()) || users.isSuperuser(auth.getPrincipal()))
			? new UserProfile(user).toJson()
			: new UserInfo(user).toJson();
	}

	@BodyParser.Of(BodyParser.Json.class)
	public Result update(String userId) {
		ObjectNode body = body();
		User user = new UserLookup(users).getUser(userId);
    	if (user == null) {
    		return notFound("user not found");
    	}
    	UpdateUserForm form = new UpdateUserForm(body);
    	if (form.getEmail() != null) {
    		return updateEmail(form, user);
    	}
    	if (form.getPassword() != null) {
    		return updatePassword(form, user);
    	}
    	if (form.isVerified()) {
    		return updateVerified(form, user);
    	}
    	if (form.getQuota() != null) {
    		return updateQuota(form, user);
    	}
    	return badRequest("invalid update request");
	}

	private Result updateEmail(UpdateUserForm form, User user) {
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
    	if (auth.getScope() != null || !user.is(auth.getPrincipal()) && !users.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
		String email = form.getEmail();
    	if (!SignUpForm.isValidEmail(email)) {
    		return badRequest("invalid email address");
    	}
		String commandId = dispatcher.dispatch(new ChangeUserEmailCommand(auth.getPrincipal(), user.getName(), user.getEmail(), email, user.isVerified(), user.isVerified() && user.getEmail().equals(email)));
		mailer.send(user.getName(), email);
		response().setHeader(COMMAND_ID, commandId);
		return noContent();
	}

	private Result updatePassword(UpdateUserForm form, final User user) {
    	String key = form.getKey();
    	if (key == null) {
    		return badRequest("missing key field");
    	}
    	String password = form.getPassword();
		if (!SignUpForm.isValidPassword(password)) {
			return badRequest("invalid password");
		}
    	String expires = form.getExpires();
		if (expires == null) {
			return badRequest("missing expires field");
		}
		if (!new PasswordResetKey(user, expires).validate(key)) {
			return badRequest("invalid key");
		}
		Authorization auth = new Authorization(user.asIdentity(), null, null);
		final CompoundCommand command = new CompoundCommand(user.asIdentity(), "updated password", "reverted password");
		command.add(new ChangeUserPasswordCommand(user.asIdentity(), user.getName(), user.getHashedPassword(), User.hashPassword(password)));
		command.add(new CreateAuthorizationCommand(user.asIdentity(), auth));
		AuthorizationQuery query = new AuthorizationQuery()
			.principalEqualTo(user.asIdentity())
			.clientIsNull();
		authorizations.find(query, new Callback<Authorization>() {
			@Override
			public void call(Authorization authorization) {
				command.add(new DeleteAuthorizationCommand(user.asIdentity(), authorization));
			}
		});
		String commandId = dispatcher.dispatch(command);
		response().setHeader(COMMAND_ID, commandId);
		return ok(Nodes.newObject("access_token", auth.getId()));
	}

	private Result updateVerified(UpdateUserForm form, User user) {
		if (user.isVerified()) {
			return badRequest("already verified");
		}
		String key = form.getKey();
		if (key == null) {
			return badRequest("missing key");
		}
		if (!new EmailVerificationKey(user.getName(), user.getEmail()).validate(key)) {
			return badRequest("invalid key");
		}
		String commandId = dispatcher.dispatch(new ChangeUserVerifiedCommand(user.asIdentity(), user.getName(), true));
		response().setHeader(COMMAND_ID, commandId);
		return noContent();
	}

	private Result updateQuota(UpdateUserForm form, User user) {
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
		String commandId = dispatcher.dispatch(new ChangeQuotaCommand(auth.getPrincipal(), user.getName(), user.getQuota(), form.getQuota()));
		response().setHeader(COMMAND_ID, commandId);
		return noContent();
	}
}
