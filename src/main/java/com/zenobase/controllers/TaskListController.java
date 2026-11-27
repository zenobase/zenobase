package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.commands.CreateTaskCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.TaskQuery;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.TaskRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserLookup;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskList;
import com.zenobase.tasks.TaskManager;
import com.zenobase.tasks.TaskManagerRegistry;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;

public class TaskListController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final TaskManagerRegistry registry;
	private final TaskRepository tasks;
	private final BucketRepository buckets;
	private final UserRepository users;

	@Inject
	public TaskListController(
		AuthorizationContext security,
		CommandDispatcher dispatcher,
		TaskManagerRegistry registry,
		TaskRepository tasks,
		BucketRepository buckets,
		UserRepository users
	) {
		super(security);
		this.dispatcher = dispatcher;
		this.registry = registry;
		this.tasks = tasks;
		this.buckets = buckets;
		this.users = users;
	}

	public void findAll(ServerRequest req, ServerResponse res) {
		String q = req.query().first("q").orElse(null);
		int offset = Integer.parseInt(req.query().first("offset").orElse("0"));
		int limit = Integer.parseInt(req.query().first("limit").orElse("10"));
		requirePaging(offset, limit);
		Authorization auth = requireAuthorization(req);
		requireFirstPartySuperuser(users, auth);
		var query = new TaskQuery();
		if (q != null) {
			query = query.queryString(q);
		}
		sendOk(res, TaskList.toJson(tasks.find(query, offset, limit)));
	}

	public void findByBucket(ServerRequest req, ServerResponse res) {
		String bucketId = req.path().pathParameters().get("bucketId");
		int offset = Integer.parseInt(req.query().first("offset").orElse("0"));
		int limit = Integer.parseInt(req.query().first("limit").orElse("10"));
		requirePaging(offset, limit);
		Authorization auth = requireAuthorization(req);
		Bucket bucket = requireFound(buckets.find(bucketId), "bucket not found");
		requirePermission(bucket.hasRole(auth, Role.OWNER) || isFirstPartySuperuser(users, auth));
		var query = new TaskQuery().bucketEqualTo(bucketId);
		sendOk(res, TaskList.toJson(tasks.find(query, TaskQuery.orderByCreated(true), offset, limit)));
	}

	public void findByUser(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		String q = req.query().first("q").orElse(null);
		int offset = Integer.parseInt(req.query().first("offset").orElse("0"));
		int limit = Integer.parseInt(req.query().first("limit").orElse("10"));
		requirePaging(offset, limit);
		Authorization auth = requireAuthorization(req);
		requireFirstParty(auth);
		Identity principal = requireFound(new UserLookup(users).getIdentity(userId), "user not found");
		requireSelfOrSuperuser(users, auth, principal);
		var query = new TaskQuery().principalEqualTo(principal);
		if (q != null) {
			query = query.queryString(q);
		}
		sendOk(res, TaskList.toJson(tasks.find(query, offset, limit)));
	}

	public void post(ServerRequest req, ServerResponse res) {
		Authorization auth = requireAuthorization(req);
		requireFirstParty(auth);
		var form = new CreateTaskForm(body(req));
		if (!form.valid()) {
			throw ResponseStatusException.badRequest("bad request");
		}
		Bucket bucket = buckets.find(form.getBucketId());
		if (bucket == null) {
			throw ResponseStatusException.badRequest("bucket not found");
		}
		requireRole(bucket, auth, Role.OWNER);
		requireNotArchived(bucket);
		if (!registry.exists(form.getType())) {
			throw ResponseStatusException.badRequest("unknown task type");
		}
		TaskManager manager = registry.find(form.getType());
		ObjectNode settings = form.getSettings();
		if (settings == null) {
			settings = Nodes.newObject();
		}
		requireNotSuspended(auth);
		Task task = manager.newTask(form.getBucketId(), auth.getPrincipal(), settings);
		String commandId = dispatcher.dispatch(new CreateTaskCommand(auth.getPrincipal(), task));
		setHeader(res, LOCATION, "/tasks/" + task.getId());
		setHeader(res, COMMAND_ID, commandId);
		sendCreated(res, task.toJson());
	}
}
