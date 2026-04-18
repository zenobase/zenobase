package com.zenobase.jobs;

import com.google.common.collect.ImmutableList;
import com.zenobase.services.Bus;
import jakarta.inject.Inject;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.joda.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler {

	private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

	private final Bus bus;
	private final ImmutableList<Job> jobs;
	private final ScheduledExecutorService executor;

	@Inject
	public Scheduler(Bus bus, Set<Job> jobs) {
		this(bus, jobs, Executors.newScheduledThreadPool(1));
	}

	Scheduler(Bus bus, Set<Job> jobs, ScheduledExecutorService executor) {
		this.bus = bus;
		this.jobs = ImmutableList.copyOf(jobs);
		this.executor = executor;
	}

	public void start() {
		logger.info("Scheduling jobs...");
		for (Job job : this.jobs) {
			schedule(job);
		}
	}

	private void schedule(Job job) {
		schedule(job.getBegin(), job.getPeriod(), () -> {
			if (!bus.isReadOnly() && !bus.isSchedulerDisabled()) {
				try {
					job.run();
				} catch (Exception e) {
					logger.error("Could not run job: {}", job.getLabel(), e);
				}
			}
		});
	}

	@SuppressWarnings("FutureReturnValueIgnored")
	private void schedule(LocalTime begin, Period repeat, Runnable runnable) {
		Duration initialDelay = nextExecution(DateTime.now(DateTimeZone.UTC), begin, repeat);
		executor.scheduleAtFixedRate(
			runnable,
			initialDelay.getMillis(),
			toDurationMillis(repeat),
			TimeUnit.MILLISECONDS
		);
	}

	private static Duration nextExecution(DateTime now, LocalTime begin, Period repeat) {
		DateTime next = begin.toDateTime(now);
		while (next.isBefore(now)) {
			next = next.plus(repeat);
		}
		return new Duration(now, next);
	}

	private static long toDurationMillis(Period period) {
		if (period.getDays() > 0) {
			return TimeUnit.DAYS.toMillis(period.getDays());
		} else if (period.getHours() > 0) {
			return TimeUnit.HOURS.toMillis(period.getHours());
		} else if (period.getMinutes() > 0) {
			return TimeUnit.MINUTES.toMillis(period.getMinutes());
		} else {
			throw new IllegalArgumentException("Unsupported period: " + period);
		}
	}

	public ImmutableList<Job> findJobs() {
		return jobs;
	}

	public void close() {
		executor.shutdownNow();
	}
}
