package com.zenobase.controllers;

import org.codehaus.jackson.node.ObjectNode;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;
import com.google.inject.Inject;

import com.zenobase.actions.Timed;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.services.UserRepository;

@With(Timed.class)
public class SecurityController extends ControllerSupport {

	private final UserRepository users;

	@Inject
	public SecurityController(SecurityContext security, UserRepository users) {
		super(security);
		this.users = users;
	}

	@BodyParser.Of(BodyParser.Json.class)
	public Result signIn() {
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
		getSecurityContext().setPrincipal(user.asIdentity(), form.isRemember());
		ObjectNode result = new UserInfo(user).toJson();
		result.put("hash", getSecurityContext().sign(user.getId()));
		return ok(result);
	}

	public Result signOut() {
		getSecurityContext().unsetPrincipal();
		return noContent();
	}
}
