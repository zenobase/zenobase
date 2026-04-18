package com.zenobase.controllers;

import com.zenobase.models.User;
import com.zenobase.models.UserProfile;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.UserRepository;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;

public class WhoController extends ControllerSupport {

	private final UserRepository users;

	@Inject
	public WhoController(AuthorizationContext security, UserRepository users) {
		super(security);
		this.users = users;
	}

	public void who(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendNoContent(res);
			return;
		}
		User user = users.find(auth.getPrincipal());
		if (user == null) {
			sendUnauthorized(res);
			return;
		}
		sendOk(res, new UserProfile(user).toJson());
	}
}
