package com.zenobase.controllers;

import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import com.zenobase.json.Nodes;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.Job;
import com.zenobase.services.Scheduler;
import com.zenobase.services.UserRepository;

public class SchedulerController extends ControllerSupport {

	private final UserRepository users;
	private final Scheduler scheduler;

	public SchedulerController(AuthorizationContext security, UserRepository users, Scheduler scheduler) {
		super(security);
		this.users = users;
		this.scheduler = scheduler;
	}

	public void findAll(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null || !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		sendOk(res, toJson(scheduler.findJobs()));
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
