package com.zenobase.controllers;

import play.data.Form;
import play.mvc.Result;
import play.mvc.With;
import com.google.inject.Inject;

import com.zenobase.common.SecurityContext;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.services.UserManager;

@With(Timed.class)
public class SecurityController extends ControllerSupport {

	@Inject
	static UserManager users;

	public static Result signIn() {
		Form<SignInForm> form = form(SignInForm.class);
		SignInForm signIn = form.bindFromRequest().get();
		if (form.hasErrors()) {
			return badRequest();
		}
		User user = users.find(signIn.getUsername());
		if (user == null || user.isSuspended() || !user.passwordEquals(signIn.getPassword())) {
			return unauthorized();
		}
		new SecurityContext(ctx()).setPrincipal(user.asIdentity(), signIn.isRemember());
		return ok(new UserInfo(user).toJson());
	}

	public static Result signOut() {
		new SecurityContext(ctx()).unsetPrincipal();
		return noContent();
	}
}
