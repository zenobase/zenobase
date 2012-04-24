package com.zenobase.controllers;

import org.codehaus.jackson.node.ObjectNode;
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
		ObjectNode body = body();
		if (body == null) {
			return badRequest("missing request body");
		}
		SignInForm form = new SignInForm(body);
		if (!form.valid()) {
			return badRequest("invalid request body");
		}
		User user = users.find(form.getUsername());
		if (user == null || user.isSuspended() || !user.passwordEquals(form.getPassword())) {
			return unauthorized();
		}
		new SecurityContext(ctx()).setPrincipal(user.asIdentity(), form.isRemember());
		return ok(new UserInfo(user).toJson());
	}

	public static Result signOut() {
		new SecurityContext(ctx()).unsetPrincipal();
		return noContent();
	}
}
