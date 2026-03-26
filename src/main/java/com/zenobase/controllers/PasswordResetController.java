package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;

import com.zenobase.json.TokenField;
import com.zenobase.mail.PasswordResetMailer;
import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class PasswordResetController extends ControllerSupport {

	static final TokenField USERNAME = new TokenField("username");

	private final UserRepository users;
	private final PasswordResetMailer resetMailer;

	@Inject
	public PasswordResetController(
			AuthorizationContext security, UserRepository users, PasswordResetMailer resetMailer) {
		super(security);
		this.users = users;
		this.resetMailer = resetMailer;
	}

	public void requestReset(ServerRequest req, ServerResponse res) {
		ObjectNode body = body(req);
		String username = USERNAME.getValue(body);
		if (username == null) {
			sendBadRequest(res, "missing user name");
			return;
		}
		User user = users.find(username);
		if (user == null) {
			sendBadRequest(res, "user not found");
			return;
		}
		if (!user.isVerified()) {
			sendBadRequest(res, "can't reset password without a verified email address");
			return;
		}
		String email = User.EMAIL.getValue(body);
		if (!user.getEmail().equals(email)) {
			sendBadRequest(res, "invalid email");
			return;
		}
		resetMailer.send(user);
		sendNoContent(res);
	}
}
