package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;

import com.zenobase.models.User;
import com.zenobase.models.UserProfile;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.UserRepository;

public class WhoController extends ControllerSupport {

	private final UserRepository users;

	@Inject
	public WhoController(AuthorizationContext security, UserRepository users) {
		super(security);
		this.users = users;
	}

	public Result who() {
		Authorization auth = getCurrentAuthorization();
		if (auth != null) {
			User user = users.find(auth.getPrincipal());
			return ok(user != null ? new UserProfile(user).toJson() : auth.getPrincipal().toJson());
		}
    	return noContent();
    }
}
