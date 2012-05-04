package com.zenobase.controllers;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;
import com.google.inject.Inject;

import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.services.UserRepository;

@With(Timed.class)
public class SecurityController extends ControllerSupport {

	@Inject
	static UserRepository users;

	@BodyParser.Of(BodyParser.Json.class)
	public static Result signIn() {
		SignInForm form = new SignInForm(body());
		if (!form.valid()) {
			return badRequest("invalid request body");
		}
		User user = users.find(form.getUsername());
		if (user == null || !user.passwordEquals(form.getPassword())) {
			return unauthorized("invalid username or password");
		}
		if (user.isSuspended()) {
			return unauthorized("user suspended");
		}
		auth.setPrincipal(user.asIdentity(), form.isRemember());
		return ok(new UserInfo(user).toJson());
	}

	public static Result signOut() {
		auth.unsetPrincipal();
		return noContent();
	}
}
