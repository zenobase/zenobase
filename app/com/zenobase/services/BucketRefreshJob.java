package com.zenobase.services;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import play.Logger;

import com.zenobase.common.Callback;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.models.User;
import com.zenobase.tasks.CredentialsException;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskRefresher;

public class BucketRefreshJob extends Job {

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
		Logger.warn("Refreshing buckets...");
		buckets.find(new BucketQuery().isRefreshable(), bucket -> {
			User owner = users.find(Iterables.getOnlyElement(bucket.getPrincipals(Role.OWNER)));
			if (owner.getQuota() == null) {
				Logger.warn("Bucket owner does not have refresh privileges: {}", owner.getName());
				return;
			}
			try {
				for (Task task : tasks.find(new TaskQuery().bucketEqualTo(bucket.getId()), TaskQuery.orderByCreated(true), 0, 100)) {
					refresher.refresh(task);
				}
			} catch (CredentialsException e) {
				Logger.warn("Bucket owner needs to update credentials: {}", owner.getName());
			} catch (RuntimeException e) {
				Logger.error("Couldn't refresh bucket {} for: {}", bucket.getId(), owner.getName(), e);
			}
		});
		Logger.warn("Refreshed all buckets in {} ms", timer.elapsed(TimeUnit.MILLISECONDS));
	}
}
