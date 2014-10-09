package com.zenobase.controllers;

import javax.inject.Inject;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;
import com.fasterxml.jackson.databind.JsonNode;

import com.zenobase.models.StatusInfo;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.HazelcastManager;
import com.zenobase.services.IndexManager;

public class StatusController extends ControllerSupport {

	private final IndexManager manager;
	private final CommandRepository history;
	private final HazelcastManager hazelcast;

	@Inject
	public StatusController(AuthorizationContext security, IndexManager manager, CommandRepository history, HazelcastManager hazelcast) {
		super(security);
		this.manager = manager;
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
		// TODO authorize!
		JsonNode node = body().path("read_only");
		if (!node.isBoolean()) {
			return badRequest();
		}
		hazelcast.setReadOnly(node.booleanValue());
        return noContent();
    }
}
