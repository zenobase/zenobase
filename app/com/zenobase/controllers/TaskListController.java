package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.commands.CreateTaskCommand;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.TaskRepository;
import com.zenobase.services.UserRepository;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskManager;
import com.zenobase.tasks.TaskManagerRegistry;

@With(Timed.class)
public class TaskListController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final TaskManagerRegistry registry;
	private final TaskRepository tasks;
	private final BucketRepository buckets;
	private final UserRepository users;

	@Inject
	public TaskListController(SecurityContext security, CommandDispatcher dispatcher,
		TaskManagerRegistry registry, TaskRepository tasks, BucketRepository buckets, UserRepository users) {

		super(security);
		this.dispatcher = dispatcher;
		this.registry = registry;
		this.tasks = tasks;
		this.buckets = buckets;
		this.users = users;
	}

	public Result find(String field, String value, int offset, int limit) {
		if (limit > 100) {
			return badRequest("limit can't be more than 100");
		}
		Identity principal = getSecurityContext().getPrincipal();
		if (!Task.BUCKET.equals(field)) {
	    	if (principal == null) {
	    		return unauthorized();
	    	}
	    	if (!users.isSuperuser(principal)) {
	    		return forbidden();
	    	}
		} else {
			Bucket bucket = buckets.findBucket(value);
			if (bucket == null) {
				return badRequest("bucket not found");
			}
			if (bucket.getPermission(principal) != Permission.ALL) {
				return forbidden();
			}
		}
		return ok(tasks.findTasks(field, value, offset, limit).toJson());
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result post() {
    	Identity principal = getSecurityContext().getPrincipal(true);
    	if (principal == null) {
    		return unauthorized();
    	}
		CreateTaskForm form = new CreateTaskForm(body());
		if (!form.valid()) {
			return badRequest();
		}
		Bucket bucket = buckets.findBucket(form.getBucketId());
		if (bucket == null) {
			return badRequest("bucket not found");
		}
		if (bucket.getPermission(principal) != Permission.ALL) {
			return forbidden();
		}
    	TaskManager manager = registry.find(form.getType());
		if (manager == null) {
			return badRequest("unknown task type");
		}
    	Task task = manager.newTask(form.getBucketId(), principal);
    	String commandId = dispatcher.dispatch(new CreateTaskCommand(principal, task));
        // TODO getConfigureUrl
    	response().setHeader(LOCATION, com.zenobase.controllers.routes.TaskController.get(task.getId()).toString());
        return created(commandId);
    }
}
