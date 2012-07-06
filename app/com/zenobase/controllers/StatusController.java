package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.models.StatusInfo;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.IndexManager;

@With(Timed.class)
public class StatusController extends ControllerSupport {

	@Inject
	static IndexManager manager;

	@Inject
	static CommandRepository history;

	public static Result get() {
    	if (!Http.Context.current().request().queryString().isEmpty()) {
    		throw new RuntimeException("invalid parameters");
    	}
    	return ok(new StatusInfo(history.size(), manager.getCluster().getHealthStatus()).toJson());
    }
}
