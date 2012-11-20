package com.zenobase.controllers;

import javax.inject.Inject;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.models.StatusInfo;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.IndexManager;

@With(Timed.class)
public class StatusController extends ControllerSupport {

	private final IndexManager manager;
	private final CommandRepository history;

	@Inject
	public StatusController(SecurityContext security, IndexManager manager, CommandRepository history) {
		super(security);
		this.manager = manager;
		this.history = history;
	}

	public Result get() {
    	if (!Http.Context.current().request().queryString().isEmpty()) {
    		throw new RuntimeException("invalid parameters");
    	}
    	return ok(getStatus().toJson());
    }

	private StatusInfo getStatus() {
		ClusterHealthResponse health = manager.getCluster().getHealth();
		return new StatusInfo(history.size(), health.getStatus(), health.getNumberOfNodes());
	}
}
