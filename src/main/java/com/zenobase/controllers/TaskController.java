package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.commands.Command;
import com.zenobase.commands.DeleteTaskCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.TaskRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.tasks.IncompleteCredentialsException;
import com.zenobase.tasks.MissingCredentialsException;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskManagerRegistry;
import com.zenobase.tasks.TaskRefresher;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;

public class TaskController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final TaskManagerRegistry registry;
	private final TaskRepository tasks;
	private final BucketRepository buckets;
	private final UserRepository users;
	private final TaskRefresher refresher;
	private final Bus bus;

	@Inject
	public TaskController(
		AuthorizationContext security,
		CommandDispatcher dispatcher,
		TaskManagerRegistry registry,
		TaskRepository tasks,
		BucketRepository buckets,
		UserRepository users,
		TaskRefresher refresher,
		Bus bus
	) {
		super(security);
		this.dispatcher = dispatcher;
		this.registry = registry;
		this.tasks = tasks;
		this.buckets = buckets;
		this.users = users;
		this.refresher = refresher;
		this.bus = bus;
	}

	public void get(ServerRequest req, ServerResponse res) {
		String taskId = req.path().pathParameters().get("taskId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		Task task = tasks.find(taskId);
		if (task == null) {
			sendNotFound(res);
			return;
		}
		if (!task.isPermitted(auth) && !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		if (task.isStale() && !bus.isReadOnly()) {
			try {
				refresher.refresh(task);
			} catch (IncompleteCredentialsException e) {
				setHeader(res, "Link", "<" + e.getCredentials().getAuthorizationUrl() + ">");
			} catch (MissingCredentialsException e) {
				setHeader(res, "X-Credentials", e.getExpectedType());
			}
		}
		sendOk(res, task.toJson());
	}

	public void update(ServerRequest req, ServerResponse res) {
		String taskId = req.path().pathParameters().get("taskId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		Task task = tasks.find(taskId);
		if (task == null) {
			sendNotFound(res);
			return;
		}
		if (!task.isPermitted(auth)) {
			sendForbidden(res);
			return;
		}
		if (!registry.exists(task.getType())) {
			sendBadRequest(res, "unsupported task type: " + task.getType());
			return;
		}
		ObjectNode body = body(req);
		if (body.size() != 1) {
			sendBadRequest(res, "no data");
			return;
		}
		Command command = null;
		ObjectNode settings = Task.SETTINGS.getValue(body);
		if (settings != null) {
			command = UpdateTaskCommand.builder(task).set(Task.SETTINGS, task.getSettings(), settings).build();
		}
		if (command == null) {
			sendBadRequest(res, "nothing to do");
			return;
		}
		String commandId = dispatcher.dispatch(command);
		setHeader(res, COMMAND_ID, commandId);
		sendNoContent(res);
	}

	public void delete(ServerRequest req, ServerResponse res) {
		String taskId = req.path().pathParameters().get("taskId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		Task task = tasks.find(taskId);
		if (task == null) {
			sendNotFound(res);
			return;
		}
		Bucket bucket = buckets.find(task.getBucketId());
		if (bucket != null && !bucket.hasRole(auth, Role.OWNER) && !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		String commandId = dispatcher.dispatch(new DeleteTaskCommand(auth.getPrincipal(), task));
		setHeader(res, COMMAND_ID, commandId);
		sendNoContent(res);
	}
}
