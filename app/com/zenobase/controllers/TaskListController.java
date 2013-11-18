package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;

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
	public TaskListController(AuthorizationContext security, CommandDispatcher dispatcher,
		TaskManagerRegistry registry, TaskRepository tasks, BucketRepository buckets, UserRepository users) {

		super(security);
		this.dispatcher = dispatcher;
		this.registry = registry;
		this.tasks = tasks;
		this.buckets = buckets;
		this.users = users;
	}

	public Result findAll(int offset, int limit) {
		if (offset < 0 || offset > 1000) {
			return badRequest("expected offset in [0..1000]");
		}
		if (limit < 0 || limit > 100) {
			return badRequest("expected limit in [0..100]");
		}
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
		if (auth.getScope() != null) {
    		return forbidden();
		}
		if (!users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		return ok(TaskList.toJson(tasks.find(offset, limit)));
    }

	public Result findByBucket(String bucketId, int offset, int limit) {
		if (offset < 0 || offset > 1000) {
			return badRequest("expected offset in [0..1000]");
		}
		if (limit < 0 || limit > 100) {
			return badRequest("expected limit in [0..100]");
		}
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
		Bucket bucket = buckets.find(bucketId);
		if (bucket == null) {
			return notFound("bucket not found");
		}
		if (!bucket.hasRole(auth, Role.OWNER) && !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		return ok(TaskList.toJson(tasks.find(new TaskQuery().bucketEqualTo(bucketId), offset, limit)));
    }

	public Result findByUser(String userId, int offset, int limit) {
		if (offset < 0 || offset > 1000) {
			return badRequest("expected offset in [0..1000]");
		}
		if (limit < 0 || limit > 100) {
			return badRequest("expected limit in [0..100]");
		}
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
		if (auth.getScope() != null) {
    		return forbidden();
		}
		Identity principal = new UserLookup(users).getIdentity(userId);
		if (principal == null) {
			return notFound("user not found");
		}
		if (!auth.getPrincipal().equals(principal) && !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		return ok(TaskList.toJson(tasks.find(new TaskQuery().principalEqualTo(principal), offset, limit)));
    }

	@BodyParser.Of(BodyParser.Json.class)
    public Result post() {
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
		CreateTaskForm form = new CreateTaskForm(body());
		if (!form.valid()) {
			return badRequest();
		}
		Bucket bucket = buckets.find(form.getBucketId());
		if (bucket == null) {
			return badRequest("bucket not found");
		}
		if (!bucket.hasRole(auth, Role.OWNER)) {
			return forbidden();
		}
    	TaskManager manager = registry.find(form.getType());
		if (manager == null) {
			return badRequest("unknown task type");
		}
    	Task task = manager.newTask(form.getBucketId(), auth.getPrincipal(), form.getSettings());
    	String commandId = dispatcher.dispatch(new CreateTaskCommand(auth.getPrincipal(), task));
    	response().setHeader(LOCATION, com.zenobase.controllers.routes.TaskController.get(task.getId()).toString());
    	response().setHeader(COMMAND_ID, commandId);
        return created(task.toJson());
    }
}
