package com.zenobase.services;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.models.Role;
import com.zenobase.models.User;
import com.zenobase.tasks.CredentialsException;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskRefresher;

public class BucketRefreshJob extends Job {

	private static final Logger logger = LoggerFactory.getLogger(BucketRefreshJob.class);

	private final BucketRepository buckets;
	private final UserRepository users;
	private final TaskRepository tasks;
	private final TaskRefresher refresher;

	@Inject
	public BucketRefreshJob(BucketRepository buckets, UserRepository users, TaskRepository tasks, TaskRefresher refresher) {
		super("refresh buckets", new LocalTime(2, 0), Period.hours(6));
		this.buckets = buckets;
		this.users = users;
		this.tasks = tasks;
		this.refresher = refresher;
	}

	@Override
	public void run() {
		Stopwatch timer = Stopwatch.createStarted();
		var counter = new AtomicInteger();
		logger.warn("Refreshing buckets...");
		buckets.find(new BucketQuery().isRefreshable(), bucket -> {
			User owner = users.find(Iterables.getOnlyElement(bucket.getPrincipals(Role.OWNER)));
			if (hasRefreshPrivilege(owner)) {
				try {
					for (Task task : tasks.find(new TaskQuery().bucketEqualTo(bucket.getId()), TaskQuery.orderByCreated(true), 0, 100)) {
						refresher.refresh(task);
						counter.incrementAndGet();
					}
				} catch (CredentialsException e) {
					logger.warn("Couldn't refresh bucket {} for {}: {}", bucket.getId(), owner.getName(), e.getMessage());
				} catch (RuntimeException e) {
					logger.error("Couldn't refresh bucket {} for {}", bucket.getId(), owner.getName(), e);
				}
			}
		});
		logger.warn("Refreshed {} buckets in {} ms", counter, timer.elapsed(TimeUnit.MILLISECONDS));
	}

	private boolean hasRefreshPrivilege(User user) {
		return user.getQuota() != null;
	}
}
