package com.zenobase.controllers;

import java.util.List;

import javax.inject.Inject;

import play.mvc.Result;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.Job;
import com.zenobase.services.Scheduler;
import com.zenobase.services.UserRepository;

public class SchedulerController extends ControllerSupport {

	private final UserRepository users;
	private final Scheduler scheduler;

	@Inject
	public SchedulerController(AuthorizationContext security, UserRepository users, Scheduler scheduler) {
		super(security);
		this.users = users;
		this.scheduler = scheduler;
	}

	public Result findAll() {
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
		if (auth.getScope() != null || !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
        return ok(toJson(scheduler.findJobs()));
    }

	public static ObjectNode toJson(List<Job> jobs) {
    	ObjectNode resultNode = Nodes.newObject();
    	ArrayNode jobsNode = resultNode.putArray("jobs");
    	for (Job job : jobs) {
    		jobsNode.add(job.toJson());
    	}
		return resultNode;
	}
}
