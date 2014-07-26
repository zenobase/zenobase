package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;

import com.zenobase.models.SnapshotList;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.IndexManager;
import com.zenobase.services.SnapshotManager;
import com.zenobase.services.UserRepository;

public class SnapshotController extends ControllerSupport {

	private final UserRepository users;
	private final SnapshotManager manager;

	@Inject
	public SnapshotController(AuthorizationContext security, UserRepository users, IndexManager manager) {
		super(security);
		this.users = users;
		this.manager = manager.getSnapshotManager();
	}

	public Result findAll(int offset, int limit) {
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
		if (auth.getScope() != null || !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
        return ok(SnapshotList.toJson(manager.findAll(offset, limit)));
    }

	public Result snapshot() {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
		if (auth.getScope() != null || !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		manager.snapshot();
		return noContent();
	}

	public Result delete(String snapshotId) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
		if (auth.getScope() != null || !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		manager.delete(snapshotId);
		return noContent();
	}
}
