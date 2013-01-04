package com.zenobase.controllers;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.commands.Command;
import com.zenobase.commands.DeleteTaskCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.TaskRepository;
import com.zenobase.services.UserRepository;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskManager;
import com.zenobase.tasks.TaskManagerRegistry;
import com.zenobase.tasks.TaskRefresher;

@With(Timed.class)
public class TaskController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final TaskManagerRegistry registry;
	private final TaskRepository tasks;
	private final BucketRepository buckets;
	private final UserRepository users;
	private final TaskRefresher refresher;

	@Inject
	public TaskController(AuthorizationContext security, CommandDispatcher dispatcher,
		TaskManagerRegistry registry, TaskRepository tasks, BucketRepository buckets, UserRepository users, TaskRefresher refresher) {

		super(security);
		this.dispatcher = dispatcher;
		this.registry = registry;
		this.tasks = tasks;
		this.buckets = buckets;
		this.users = users;
		this.refresher = refresher;
	}

	public Result get(String taskId) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
		Task task = tasks.find(taskId);
		if (task == null) {
			return notFound();
		}
		if (!task.isPermitted(auth)) {
			return forbidden();
		}
		if (task.isStale()) {
			refresher.refresh(task);
		}
    	return ok(task.sanitized().toJson());
    }

	@BodyParser.Of(BodyParser.Json.class)
	public Result update(String taskId) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
		Task task = tasks.find(taskId);
		if (task == null) {
			return notFound();
		}
    	if (!task.isPermitted(auth)) {
    		return forbidden();
    	}
    	TaskManager manager = registry.find(task.getType());
    	if (manager == null) {
    		return internalServerError("unsupported task type: " + task.getType());
    	}
    	ObjectNode body = body();
    	if (body == null || body.size() > 1) {
    		return badRequest();
    	}
    	Command command = null;
    	ObjectNode credentials = Task.CREDENTIALS.getValue(body);
    	if (credentials != null) {
	    	command = manager.authorize(task, credentials);
    	}
    	ObjectNode settings = Task.SETTINGS.getValue(body);
    	if (settings != null) {
	    	command = UpdateTaskCommand.builder(task).set(Task.SETTINGS, task.getSettings(), settings).build();
    	}
    	if (command == null) {
    		return badRequest();
    	}
    	String commandId = dispatcher.dispatch(command);
		response().setHeader(COMMAND_ID, commandId);
    	return noContent();
    }

    public Result delete(String taskId) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
		Task task = tasks.find(taskId);
		if (task == null) {
			return notFound();
		}
    	Bucket bucket = buckets.find(task.getBucketId());
    	if (bucket == null || !bucket.hasRole(auth, Role.OWNER) && !users.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
    	String commandId = dispatcher.dispatch(new DeleteTaskCommand(auth.getPrincipal(), task));
		response().setHeader(COMMAND_ID, commandId);
    	return noContent();
    }
}
