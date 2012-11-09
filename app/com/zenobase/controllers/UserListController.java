package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.io.UserPrinter;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.services.UserRepository;

@With(Timed.class)
public class UserListController extends ControllerSupport {

	private final UserRepository users;

	@Inject
	public UserListController(SecurityContext security, UserRepository users) {
		super(security);
		this.users = users;
	}

	public Result find(String identity, int offset, int limit, boolean detail) {
		if (identity == null || detail) {
	    	Identity principal = getSecurityContext().getPrincipal();
	    	if (principal == null) {
	    		return unauthorized();
	    	}
	    	if (!users.isSuperuser(principal)) {
	    		return forbidden();
	    	}
		}
		return identity == null ? find(offset, limit) : find(new Identity(identity), detail);
    }

	private Result find(int offset, int limit) {
    	if (limit == Integer.MAX_VALUE) {
    		return findAll();
    	}
        return ok(users.find(offset, limit).toJson());
	}

	private Result findAll() {
    	Chunks<String> chunks = new StringChunks() {
			@Override
			public void onReady(final Out<String> out) {
				users.find(new UserPrinter(out));
		    	out.close();
			}
		};
        return ok(chunks);
	}

	private Result find(Identity identity, boolean detail) {
		User user = users.find(identity);
		if (user == null) {
	    	return ok(identity.toJson());
		} else if (detail) {
	    	return ok(user.toJson());
		} else {
			return ok(new UserInfo(user).toJson());
		}
    }
}
