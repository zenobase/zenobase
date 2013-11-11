package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;

import com.zenobase.io.UserPrinter;
import com.zenobase.models.UserList;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.UserRepository;

public class UserListController extends ControllerSupport {

	private final UserRepository repository;

	@Inject
	public UserListController(AuthorizationContext security, UserRepository repository) {
		super(security);
		this.repository = repository;
	}

	public Result find(int offset, int limit) {
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null || auth.getScope() != null) {
    		return unauthorized();
    	}
    	if (!repository.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
    	if (limit == Integer.MAX_VALUE) {
    		return find();
    	}
        return ok(UserList.toJson(repository.find(offset, limit)));
    }

	private Result find() {
    	Chunks<String> chunks = new StringChunks() {
			@Override
			public void onReady(final Out<String> out) {
				repository.find(new UserPrinter(out));
		    	out.close();
			}
		};
        return ok(chunks);
	}
}
