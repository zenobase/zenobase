package com.zenobase.tasks;

import com.zenobase.commands.Command;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskRefresher {

	private static final Logger logger = LoggerFactory.getLogger(TaskRefresher.class);

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
		logger.info("Refreshing: {}", task.getId());
		Bucket bucket = buckets.find(task.getBucketId());
		if (bucket == null) {
			logger.warn("Task {} references a missing bucket: {}", task.getId(), task.getBucketId());
			return;
		}
		if (!bucket.hasRole(new Authorization(task.getPrincipal()), Role.OWNER)) {
			logger.warn("Task {} does not belong to the bucket owner", task.getId());
			return;
		}
		if (!registry.exists(task.getType())) {
			logger.warn("Task {} is unsupported: {}", task.getId(), task.getType());
			return;
		}
		TaskManager manager = registry.find(task.getType());
		Command command = manager.execute(task);
		if (command != null) {
			dispatcher.dispatch(command);
		} else {
			logger.info("Task {} completed with nothing to do", task.getId());
		}
	}
}
