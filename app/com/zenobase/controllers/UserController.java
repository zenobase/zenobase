package com.zenobase.controllers;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.commands.ChangeUserEmailCommand;
import com.zenobase.commands.ChangeUserPasswordCommand;
import com.zenobase.commands.ChangeUserVerifiedCommand;
import com.zenobase.mail.VerificationMailer;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.models.UserProfile;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

@With(Timed.class)
public class UserController extends ControllerSupport {

	@Inject
	static UserRepository users;

	@Inject
	static CommandDispatcher dispatcher;

	@Inject
	static VerificationMailer mailer;

	public static Result get(String name) {
		Identity principal = auth.getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
		User user = users.find(name);
		if (user == null) {
			return notFound();
		}
		if (!user.is(principal)) {
			return forbidden();
		}
		return ok(new UserProfile(user).toJson());
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result update(String username) {
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

	private static Result updateEmail(UpdateUserForm form, User user) {
		Identity principal = auth.getPrincipal();
    	if (principal == null) {
    		return unauthorized();
    	}
    	if (!user.is(principal) && !users.isSuperuser(principal)) {
    		return forbidden();
    	}
		String email = form.getEmail();
    	if (!SignUpForm.isValidEmail(email)) {
    		return badRequest("invalid email address");
    	}
		String commandId = dispatcher.dispatch(new ChangeUserEmailCommand(principal, user.getName(), user.getEmail(), email, user.isVerified(), user.isVerified() && user.getEmail().equals(email)));
		mailer.send(user.getName(), email);
		return ok(receipt(commandId));
	}

	private static Result updatePassword(UpdateUserForm form, User user) {
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
		dispatcher.dispatch(new ChangeUserPasswordCommand(user.asIdentity(), user.getName(), user.getHashedPassword(), User.getHashedPassword(password)));
		auth.setPrincipal(user.asIdentity(), true);
		return noContent();
	}

	private static Result updateVerified(UpdateUserForm form, User user) {
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
