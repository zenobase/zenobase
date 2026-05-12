package com.zenobase.tasks;

import com.zenobase.commands.Command;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import io.sentry.ISpan;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskRefresher {

	private static final Logger logger = LoggerFactory.getLogger(TaskRefresher.class);

	private final TaskManagerRegistry registry;
	private final BucketRepository buckets;
	private final CommandDispatcher dispatcher;
	private final Bus bus;

	@Inject
	public TaskRefresher(
		TaskManagerRegistry registry,
		BucketRepository buckets,
		CommandDispatcher dispatcher,
		Bus bus
	) {
		this.registry = registry;
		this.buckets = buckets;
		this.dispatcher = dispatcher;
		this.bus = bus;
	}

	public void refresh(Task task) {
		String lockId = "task:" + task.getId();
		if (!bus.tryLock(lockId)) {
			logger.info("Skipping refresh: already in flight for {}", task.getId());
			return;
		}
		ISpan parent = Sentry.getSpan();
		ISpan span =
			parent != null
				? parent.startChild("task.refresh", task.getType())
				: Sentry.startTransaction("task.refresh", "task.refresh");
		span.setData("task.id", task.getId());
		span.setData("task.type", task.getType());
		try {
			doRefresh(task);
			span.setStatus(SpanStatus.OK);
		} catch (RuntimeException e) {
			span.setStatus(SpanStatus.INTERNAL_ERROR);
			span.setThrowable(e);
			throw e;
		} finally {
			span.finish();
			bus.unlock(lockId);
		}
	}

	private void doRefresh(Task task) {
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
		Command command;
		try {
			command = manager.execute(task);
		} catch (InvalidTokenException e) {
			if (manager instanceof OAuthTaskManager oauth) {
				dispatcher.dispatch(oauth.recoverInvalidToken(e));
				oauth.reload(e); // re-throws IncompleteCredentialsException with the new authorization URL
			}
			throw e;
		}
		if (command != null) {
			dispatcher.dispatch(command);
		} else {
			logger.info("Task {} completed with nothing to do", task.getId());
		}
	}
}
