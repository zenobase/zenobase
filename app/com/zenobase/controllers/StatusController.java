package com.zenobase.controllers;

import javax.inject.Inject;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;
import com.fasterxml.jackson.databind.JsonNode;

import com.zenobase.models.StatusInfo;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.HazelcastManager;
import com.zenobase.services.IndexManager;
import com.zenobase.services.UserRepository;

public class StatusController extends ControllerSupport {

	private final IndexManager manager;
	private final UserRepository users;
	private final CommandRepository history;
	private final HazelcastManager hazelcast;

	@Inject
	public StatusController(AuthorizationContext auth, IndexManager manager, UserRepository users, CommandRepository history, HazelcastManager hazelcast) {
		super(auth);
		this.manager = manager;
		this.users = users;
		this.history = history;
		this.hazelcast = hazelcast;
	}

	public Result get() {
    	if (!Http.Context.current().request().queryString().isEmpty()) {
    		throw new RuntimeException("invalid parameters");
    	}
    	return ok(getStatus().toJson());
    }

	private StatusInfo getStatus() {
		ClusterHealthResponse health = manager.getCluster().getHealth();
		return new StatusInfo(history.size(), health.getStatus(), health.getNumberOfNodes(), hazelcast.count(), hazelcast.isReadOnly());
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
		JsonNode node = body().path("read_only");
		if (!node.isBoolean()) {
			return badRequest();
		}
		hazelcast.setReadOnly(node.booleanValue());
        return noContent();
    }
}
