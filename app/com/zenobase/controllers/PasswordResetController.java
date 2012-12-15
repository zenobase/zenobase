package com.zenobase.controllers;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.json.TokenField;
import com.zenobase.mail.PasswordResetMailer;
import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

@With(Timed.class)
public class PasswordResetController extends ControllerSupport {

	static final TokenField USERNAME = new TokenField("username");

	private final UserRepository users;
	private final PasswordResetMailer resetMailer;

	@Inject
	public PasswordResetController(AuthorizationContext security, UserRepository users, PasswordResetMailer resetMailer) {
		super(security);
		this.users = users;
		this.resetMailer = resetMailer;
	}

	@BodyParser.Of(BodyParser.Json.class)
	public Result requestReset() {
		ObjectNode body = body();
		String username = USERNAME.getValue(body);
		if (username == null) {
			return badRequest("missing user name");
		}
		User user = users.find(username);
    	if (user == null) {
    		return badRequest("user not found");
    	}
		if (!user.isVerified()) {
			return badRequest("can't reset password without a verified email address");
		}
		String email = User.EMAIL.getValue(body);
		if (!user.getEmail().equals(email)) {
			return badRequest("invalid email");
		}
		resetMailer.send(user);
		return noContent();
	}
}
