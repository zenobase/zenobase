package com.zenobase.controllers;

import javax.inject.Inject;

import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Result;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.commands.Command;
import com.zenobase.commands.DeleteTaskCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.TaskRepository;
import com.zenobase.services.UserRepository;
import com.zenobase.tasks.IncompleteCredentialsException;
import com.zenobase.tasks.MissingCredentialsException;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskManager;
import com.zenobase.tasks.TaskManagerRegistry;
import com.zenobase.tasks.TaskRefresher;

public class TaskController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final TaskManagerRegistry registry;
	private final TaskRepository tasks;
	private final BucketRepository buckets;
	private final UserRepository users;
	private final TaskRefresher refresher;
	private final Bus bus;

	@Inject
	public TaskController(AuthorizationContext security, CommandDispatcher dispatcher,
		TaskManagerRegistry registry, TaskRepository tasks, BucketRepository buckets, UserRepository users,
		TaskRefresher refresher, Bus bus) {

		super(security);
		this.dispatcher = dispatcher;
		this.registry = registry;
		this.tasks = tasks;
		this.buckets = buckets;
		this.users = users;
		this.refresher = refresher;
		this.bus = bus;
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
		if (!task.isPermitted(auth) && !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		if (task.isStale() && !bus.isReadOnly()) {
			try {
				refresher.refresh(task);
			} catch (IncompleteCredentialsException e) {
				response().setHeader("Link", "<" + e.getCredentials().getAuthorizationUrl() + ">");
			} catch (MissingCredentialsException e) {
				response().setHeader("X-Credentials", e.getExpectedType());
	    	} catch (RuntimeException e) {
				Logger.warn("Couldn't refresh task: " + task.getId(), e);
			}
		}
    	return ok(task.toJson());
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
    		return badRequest("unsupported task type: " + task.getType());
    	}
    	ObjectNode body = body();
    	if (Nodes.size(body) != 1) {
    		return badRequest("no data");
    	}
    	Command command = null;
    	ObjectNode settings = Task.SETTINGS.getValue(body);
    	if (settings != null) {
	    	command = UpdateTaskCommand.builder(task).set(Task.SETTINGS, task.getSettings(), settings).build();
    	}
    	if (command == null) {
    		return badRequest("nothing to do");
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
    	if (bucket != null && !bucket.hasRole(auth, Role.OWNER) && !users.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
    	String commandId = dispatcher.dispatch(new DeleteTaskCommand(auth.getPrincipal(), task));
		response().setHeader(COMMAND_ID, commandId);
    	return noContent();
    }
}
