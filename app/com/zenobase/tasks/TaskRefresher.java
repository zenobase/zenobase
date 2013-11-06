package com.zenobase.tasks;

import javax.inject.Inject;

import play.Logger;

import com.zenobase.commands.Command;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;

public class TaskRefresher {

	private final TaskManagerRegistry registry;
	private final BucketRepository buckets;
	private final CommandDispatcher dispatcher;

	@Inject
	public TaskRefresher(TaskManagerRegistry registry, BucketRepository buckets, CommandDispatcher dispatcher) {
		this.registry = registry;
		this.buckets = buckets;
		this.dispatcher = dispatcher;
	}

	public void refresh(Task task) {
		Logger.info("Refreshing: " + task.getId());
		Bucket bucket = buckets.find(task.getBucketId());
		if (bucket == null) {
			return;
		}
    	if (!bucket.hasRole(new Authorization(task.getPrincipal()), Role.OWNER)) {
			return;
    	}
    	TaskManager manager = registry.find(task.getType());
    	if (manager == null) {
			return;
    	}
		Command command = manager.execute(task);
    	if (command != null) {
    		dispatcher.dispatch(command);
    	}
	}
}
