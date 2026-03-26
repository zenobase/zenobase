package com.zenobase.controllers;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;

import com.zenobase.commands.CreateTaskCommand;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.TaskQuery;
import com.zenobase.services.TaskRepository;
import com.zenobase.services.UserLookup;
import com.zenobase.services.UserRepository;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskList;
import com.zenobase.tasks.TaskManager;
import com.zenobase.tasks.TaskManagerRegistry;

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
			UserRepository users) {

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
		if (offset < 0 || offset > 1000) {
			sendBadRequest(res, "expected offset in [0..1000]");
			return;
		}
		if (limit < 0 || limit > 100) {
			sendBadRequest(res, "expected limit in [0..100]");
			return;
		}
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null) {
			sendForbidden(res);
			return;
		}
		if (!users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
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
		if (offset < 0 || offset > 1000) {
			sendBadRequest(res, "expected offset in [0..1000]");
			return;
		}
		if (limit < 0 || limit > 100) {
			sendBadRequest(res, "expected limit in [0..100]");
			return;
		}
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		Bucket bucket = buckets.find(bucketId);
		if (bucket == null) {
			sendNotFound(res, "bucket not found");
			return;
		}
		if (!bucket.hasRole(auth, Role.OWNER) && !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		var query = new TaskQuery().bucketEqualTo(bucketId);
		sendOk(res, TaskList.toJson(tasks.find(query, TaskQuery.orderByCreated(true), offset, limit)));
	}

	public void findByUser(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		String q = req.query().first("q").orElse(null);
		int offset = Integer.parseInt(req.query().first("offset").orElse("0"));
		int limit = Integer.parseInt(req.query().first("limit").orElse("10"));
		if (offset < 0 || offset > 1000) {
			sendBadRequest(res, "expected offset in [0..1000]");
			return;
		}
		if (limit < 0 || limit > 100) {
			sendBadRequest(res, "expected limit in [0..100]");
			return;
		}
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null) {
			sendForbidden(res);
			return;
		}
		Identity principal = new UserLookup(users).getIdentity(userId);
		if (principal == null) {
			sendNotFound(res, "user not found");
			return;
		}
		if (!auth.getPrincipal().equals(principal) && !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		var query = new TaskQuery().principalEqualTo(principal);
		if (q != null) {
			query = query.queryString(q);
		}
		sendOk(res, TaskList.toJson(tasks.find(query, offset, limit)));
	}

	public void post(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		var form = new CreateTaskForm(body(req));
		if (!form.valid()) {
			sendBadRequest(res, "bad request");
			return;
		}
		Bucket bucket = buckets.find(form.getBucketId());
		if (bucket == null) {
			sendBadRequest(res, "bucket not found");
			return;
		}
		if (!bucket.hasRole(auth, Role.OWNER)) {
			sendForbidden(res);
			return;
		}
		TaskManager manager = registry.find(form.getType());
		if (manager == null) {
			sendBadRequest(res, "unknown task type");
			return;
		}
		Task task = manager.newTask(form.getBucketId(), auth.getPrincipal(), form.getSettings());
		String commandId = dispatcher.dispatch(new CreateTaskCommand(auth.getPrincipal(), task));
		setHeader(res, LOCATION, "/tasks/" + task.getId());
		setHeader(res, COMMAND_ID, commandId);
		sendCreated(res, task.toJson());
	}
}
