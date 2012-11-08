package com.zenobase.controllers;

import javax.inject.Inject;

import play.Logger;
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
	public StatusController(IndexManager manager, CommandRepository history) {
		Logger.info("init status");
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
		return new StatusInfo(history.size(), manager.getCluster().getHealthStatus());
	}
}
