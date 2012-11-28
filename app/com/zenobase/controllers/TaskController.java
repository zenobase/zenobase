package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.commands.Command;
import com.zenobase.commands.DeleteTaskCommand;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
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
	public TaskController(SecurityContext security, CommandDispatcher dispatcher,
		TaskManagerRegistry registry, TaskRepository tasks, BucketRepository buckets) {

		super(security);
		this.dispatcher = dispatcher;
		this.registry = registry;
		this.tasks = tasks;
		this.buckets = buckets;
	}

	public Result get(String taskId) {
		Identity principal = getSecurityContext().getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
		Task task = tasks.findTask(taskId);
		if (task == null) {
			return notFound();
		}
		Bucket bucket = buckets.findBucket(task.getBucketId());
		if (bucket == null) {
			return notFound();
		}
    	if (bucket.getPermission(principal) != Permission.ALL) {
    		return forbidden();
    	}
    	return ok(task.toJson());
    }

    @BodyParser.Of(BodyParser.Json.class)
	public Result auth(String taskId) {
		Identity principal = getSecurityContext().getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
		Task task = tasks.findTask(taskId);
		if (task == null) {
			return notFound();
		}
		Bucket bucket = buckets.findBucket(task.getBucketId());
		if (bucket == null) {
			return notFound();
		}
    	if (bucket.getPermission(principal) != Permission.ALL) {
    		return forbidden();
    	}
    	TaskManager manager = registry.find(task.getType());

    	if (body().size() == 0) {
        	String authorizationUrl = manager.getAuthorizationUrl(task);
    		if (authorizationUrl == null) {
    			return badRequest();
    		}
    		return redirect(authorizationUrl);
    	}

    	Command command = manager.authorize(task, body());
    	if (command == null) {
    		return badRequest();
    	}
		String commandId = dispatcher.dispatch(command);
		return success(commandId);
    }

	public Result run(String taskId) {
		Identity principal = getSecurityContext().getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
		Task task = tasks.findTask(taskId);
		if (task == null) {
			return notFound();
		}
		Bucket bucket = buckets.findBucket(task.getBucketId());
		if (bucket == null) {
			return notFound();
		}
    	if (bucket.getPermission(principal) != Permission.ALL) {
    		return forbidden();
    	}
		if (task.getState() != Task.State.READY) {
			return badRequest("task is not ready");
		}
    	TaskManager manager = registry.find(task.getType());
    	Command command = manager.execute(task);
    	if (command != null) {
    		String commandId = dispatcher.dispatch(command);
    		return success(commandId);
    	}
    	return status(ACCEPTED);
    }

    public Result delete(String taskId) {
    	Identity principal = getSecurityContext().getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
		Task task = tasks.findTask(taskId);
		if (task == null) {
			return notFound();
		}
    	Bucket bucket = buckets.findBucket(task.getBucketId());
    	if (bucket == null) {
    		return notFound();
    	}
    	if (bucket.getPermission(principal) != Permission.ALL) {
    		return forbidden();
    	}
    	String commandId = dispatcher.dispatch(new DeleteTaskCommand(principal, task));
    	return success(commandId);
    }
}
