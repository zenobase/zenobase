package com.zenobase.controllers;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.commands.ChangeUserEmailCommand;
import com.zenobase.commands.ChangeUserPasswordCommand;
import com.zenobase.commands.ChangeUserVerifiedCommand;
import com.zenobase.commands.CreateAuthorizationCommand;
import com.zenobase.json.Nodes;
import com.zenobase.mail.VerificationMailer;
import com.zenobase.models.User;
import com.zenobase.models.UserProfile;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

@With(Timed.class)
public class UserController extends ControllerSupport {

	private final UserRepository users;
	private final CommandDispatcher dispatcher;
	private final VerificationMailer mailer;

	@Inject
	public UserController(AuthorizationContext security, UserRepository users,
		CommandDispatcher dispatcher, VerificationMailer mailer) {

		super(security);
		this.users = users;
		this.dispatcher = dispatcher;
		this.mailer = mailer;
	}

	public Result get(String name) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
		User user = users.find(name);
		if (user == null) {
			return notFound();
		}
		if (auth.getScope() != null || !(user.is(auth.getPrincipal()) || users.isSuperuser(auth.getPrincipal()))) {
			return forbidden();
		}
		return ok(new UserProfile(user).toJson());
	}

	@BodyParser.Of(BodyParser.Json.class)
	public Result update(String username) {
		ObjectNode body = body();
		User user = users.find(username);
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
		return success(commandId);
	}

	private Result updatePassword(UpdateUserForm form, User user) {
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
		dispatcher.dispatch(new ChangeUserPasswordCommand(user.asIdentity(), user.getName(), user.getHashedPassword(), User.getHashedPassword(password)));
		dispatcher.dispatch(new CreateAuthorizationCommand(user.asIdentity(), auth));
		ObjectNode result = Nodes.newObject();
		result.put("access_token", auth.getId());
		return ok(result);
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
		dispatcher.dispatch(new ChangeUserVerifiedCommand(user.asIdentity(), user.getName(), true));
		return noContent();
	}
}
