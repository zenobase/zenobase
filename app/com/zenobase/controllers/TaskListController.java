package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.google.common.base.Strings;
import com.zenobase.actions.Timed;
import com.zenobase.commands.CreateTaskCommand;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.search.QueryConstraint;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.TaskRepository;
import com.zenobase.services.UserRepository;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskList;
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
	public TaskListController(AuthorizationContext security, CommandDispatcher dispatcher,
		TaskManagerRegistry registry, TaskRepository tasks, BucketRepository buckets, UserRepository users) {

		super(security);
		this.dispatcher = dispatcher;
		this.registry = registry;
		this.tasks = tasks;
		this.buckets = buckets;
		this.users = users;
	}

	public Result find(String query, int offset, int limit) {
		if (limit > 100) {
			return badRequest("limit can't be more than 100");
		}
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
		if (auth.getScope() != null) {
    		return forbidden();
		}
		QueryConstraint constraint = null;
		if (!Strings.isNullOrEmpty(query)) {
			try {
				constraint = QueryConstraint.parse(query);
			} catch (IllegalArgumentException e) {
				return badRequest("query is malformed");
			}
		}
		if (!isConstrainedToPrincipal(constraint, auth.getPrincipal()) && !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		return constraint != null
			? ok(TaskList.toJson(tasks.find(constraint.getField(), constraint.getValue(), offset, limit)))
			: ok(TaskList.toJson(tasks.find(offset, limit)));
    }

	private static boolean isConstrainedToPrincipal(QueryConstraint constraint, Identity principal) {
		return constraint != null
			&& Task.PRINCIPAL.getName().equals(constraint.getField())
			&& principal.getId().equals(constraint.getValue());
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
        return created(commandId);
    }
}
