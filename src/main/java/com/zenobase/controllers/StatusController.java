package com.zenobase.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;

import com.zenobase.models.StatusInfo;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.Bus;
import com.zenobase.services.UserRepository;

public class StatusController extends ControllerSupport {

	private final UserRepository users;
	private final Bus bus;

	@Inject
	public StatusController(AuthorizationContext auth, UserRepository users, Bus bus) {
		super(auth);
		this.users = users;
		this.bus = bus;
	}

	public void get(ServerRequest req, ServerResponse res) {
		if (!req.query().isEmpty()) {
			throw new RuntimeException("invalid parameters");
		}
		sendOk(res, new StatusInfo(bus.isReadOnly(), bus.isSchedulerDisabled()).toJson());
	}

	public void post(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null) {
			sendForbidden(res);
			return;
		}
		if (!users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		JsonNode body = body(req);
		setReadOnly(body.path("read_only"));
		setSchedulerDisabled(body.path("scheduler_disabled"));
		sendNoContent(res);
	}

	private void setReadOnly(JsonNode node) {
		if (node.isBoolean()) {
			bus.setReadOnly(node.booleanValue());
		}
	}

	private void setSchedulerDisabled(JsonNode node) {
		if (node.isBoolean()) {
			bus.setSchedulerDisabled(node.booleanValue());
		}
	}
}
