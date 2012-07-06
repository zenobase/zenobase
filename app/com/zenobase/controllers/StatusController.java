package com.zenobase.controllers;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.json.Nodes;
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
		ObjectNode node = Nodes.newObject();
		node.put("queue", history.size());
		node.put("status", manager.getCluster().getHealthStatus().toString());
    	return ok(node);
    }
}
