package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;

import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.services.UserRepository;

public class WhoController extends ControllerSupport {

	@Inject
	static UserRepository users;

	public static Result who() {
		Identity principal = auth.getPrincipal();
		if (principal != null) {
			User user = users.find(principal);
			return ok(user != null ? new UserInfo(user).toJson() : principal.toJson());
		}
    	return noContent();
    }
}
