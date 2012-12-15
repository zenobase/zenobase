package com.zenobase.controllers;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;
import com.google.common.base.Objects;

import com.zenobase.actions.Timed;
import com.zenobase.commands.Command;
import com.zenobase.commands.DeleteTaskCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Bucket;
import com.zenobase.models.Permission;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.TaskRepository;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskManager;
import com.zenobase.tasks.TaskManagerRegistry;

@With(Timed.class)
public class TaskController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final TaskManagerRegistry registry;
	private final TaskRepository tasks;
	private final BucketRepository buckets;

	@Inject
	public TaskController(AuthorizationContext security, CommandDispatcher dispatcher,
		TaskManagerRegistry registry, TaskRepository tasks, BucketRepository buckets) {

		super(security);
		this.dispatcher = dispatcher;
		this.registry = registry;
		this.tasks = tasks;
		this.buckets = buckets;
	}

	public Result get(String taskId) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
		Task task = tasks.findTask(taskId);
		if (task == null) {
			return notFound();
		}
		if (!task.isPermitted(auth)) {
			return forbidden();
		}
		if (isStale(task)) {
			refresh(task);
		}
    	return ok(task.sanitized().toJson());
    }

	private boolean isStale(Task task) {
		DateTime completed = Objects.firstNonNull(task.getCompleted(), new DateTime(0L));
		return task.isEnabled() && Minutes.minutesBetween(completed, DateTime.now()).isGreaterThan(Minutes.ONE);
	}

	private void refresh(Task task) {
		Logger.info("Refreshing: " + task.getId());
		Bucket bucket = buckets.findBucket(task.getBucketId());
		if (bucket == null) {
			task.setStatus(Task.Status.FAILED);
			return;
		}
    	if (!bucket.isPermitted(new Authorization(task.getPrincipal()), Permission.ALL)) {
			task.setStatus(Task.Status.FAILED);
			return;
    	}
    	TaskManager manager = registry.find(task.getType());
    	if (manager == null) {
			task.setStatus(Task.Status.FAILED);
			return;
    	}
    	Command command = manager.execute(task);
    	if (command != null) {
    		dispatcher.dispatch(command);
    	}
	}

    @BodyParser.Of(BodyParser.Json.class)
	public Result update(String taskId) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
		Task task = tasks.findTask(taskId);
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
    	return success(commandId);
    }

    public Result delete(String taskId) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
		Task task = tasks.findTask(taskId);
		if (task == null) {
			return notFound();
		}
    	Bucket bucket = buckets.findBucket(task.getBucketId());
    	if (bucket != null && !bucket.isPermitted(auth, Permission.ALL)) {
    		return forbidden();
    	}
    	String commandId = dispatcher.dispatch(new DeleteTaskCommand(auth.getPrincipal(), task));
    	return success(commandId);
    }
}
