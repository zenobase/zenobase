package com.zenobase.controllers;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;

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
	public StatusController(AuthorizationContext auth, IndexManager manager, UserRepository users, CommandRepository history, Bus bus) {
		super(auth);
		this.manager = manager;
		this.users = users;
		this.history = history;
		this.bus = bus;
	}

	public Result get() {
    	if (!Http.Context.current().request().queryString().isEmpty()) {
    		throw new RuntimeException("invalid parameters");
    	}
    	StatusInfo statusInfo = getStatus();
    	JsonNode json = statusInfo.toJson();
    	switch (statusInfo.getHealth()) {
    		case Red:
    			return status(503, json);
    		default:
    			return ok(json);
    	}
    }

	private StatusInfo getStatus() {
		HealthResponse health = manager.getCluster().getHealth();
		return new StatusInfo(history.size(), health.status(), health.numberOfNodes(), bus.isReadOnly(), bus.isSchedulerDisabled());
	}

	@BodyParser.Of(BodyParser.Json.class)
    public Result post() {
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
		if (auth.getScope() != null) {
    		return forbidden();
		}
		if (!users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		JsonNode body = body();
		setReadOnly(body.path("read_only"));
		setSchedulerDisabled(body.path("scheduler_disabled"));
        return noContent();
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
