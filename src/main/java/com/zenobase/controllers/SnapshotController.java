package com.zenobase.controllers;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;

import com.zenobase.oauth.Authorization;
import com.zenobase.services.IndexManager;
import com.zenobase.services.SnapshotList;
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

	public void findAll(ServerRequest req, ServerResponse res) {
		int offset = Integer.parseInt(req.query().first("offset").orElse("0"));
		int limit = Integer.parseInt(req.query().first("limit").orElse("10"));
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null || !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		sendOk(res, SnapshotList.toJson(manager.findAll(offset, limit)));
	}

	public void snapshot(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null || !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		manager.snapshot();
		sendNoContent(res);
	}

	public void delete(ServerRequest req, ServerResponse res) {
		String snapshotId = req.path().pathParameters().get("snapshotId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null || !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		manager.delete(snapshotId);
		sendNoContent(res);
	}
}
