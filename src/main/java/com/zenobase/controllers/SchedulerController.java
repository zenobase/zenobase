package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.jobs.Job;
import com.zenobase.jobs.Scheduler;
import com.zenobase.json.Nodes;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.UserRepository;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import java.util.List;

public class SchedulerController extends ControllerSupport {

	private final UserRepository users;
	private final Scheduler scheduler;

	@Inject
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
