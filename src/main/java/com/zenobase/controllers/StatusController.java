package com.zenobase.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import org.opensearch.client.opensearch.cluster.HealthResponse;

import com.zenobase.models.StatusInfo;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.IndexManager;
import com.zenobase.services.UserRepository;

public class StatusController extends ControllerSupport {

	private final IndexManager manager;
	private final UserRepository users;
	private final CommandRepository history;
	private final Bus bus;

	@Inject
	public StatusController(
			AuthorizationContext auth, IndexManager manager, UserRepository users, CommandRepository history, Bus bus) {
		super(auth);
		this.manager = manager;
		this.users = users;
		this.history = history;
		this.bus = bus;
	}

	public void get(ServerRequest req, ServerResponse res) {
		if (!req.query().isEmpty()) {
			throw new RuntimeException("invalid parameters");
		}
		StatusInfo statusInfo = getStatus();
		JsonNode json = statusInfo.toJson();
		switch (statusInfo.getHealth()) {
			case Red -> sendStatus(res, 503, json);
			default -> sendOk(res, json);
		}
	}

	private StatusInfo getStatus() {
		HealthResponse health = manager.getCluster().getHealth();
		return new StatusInfo(
				history.size(), health.status(), health.numberOfNodes(), bus.isReadOnly(), bus.isSchedulerDisabled());
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
