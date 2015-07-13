package com.zenobase.services;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import akka.actor.Cancellable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import play.libs.Akka;
import scala.concurrent.duration.FiniteDuration;

public class Scheduler {

	private final Bus bus;
	private final ImmutableList<Job> jobs;
	private final List<Cancellable> scheduled = Lists.newArrayList();

	@Inject
	public Scheduler(Bus bus, Set<Job> jobs) {
		this.bus = bus;
		this.jobs = ImmutableList.copyOf(jobs);
		for (Job job : this.jobs) {
			schedule(job);
		}
	}

	private void schedule(final Job job) {
		schedule(job.getBegin(), job.getPeriod(), new Runnable() {
			@Override
			public void run() {
				if (bus.isMaster() && !bus.isReadOnly() && !bus.isSchedulerDisabled()) {
					job.run();
				}
			}
		});
	}

	private void schedule(LocalTime begin, Period repeat, Runnable runnable) {
		scheduled.add(Akka.system().scheduler().schedule(toDuration(nextExecution(DateTime.now(DateTimeZone.UTC), begin, repeat)),
			toDuration(repeat), runnable, Akka.system().dispatcher()));
	}

	static Duration nextExecution(DateTime now, LocalTime begin, Period repeat) {
		DateTime next = begin.toDateTime(now);
		while (next.isBefore(now)) {
			next = next.plus(repeat);
		}
		return new Duration(now, next);
	}

	private static FiniteDuration toDuration(Duration duration) {
		return FiniteDuration.create(duration.getMillis(), TimeUnit.MILLISECONDS);
	}

	private static FiniteDuration toDuration(Period period) {
		if (period.getDays() > 0) {
			return FiniteDuration.create(period.getDays(), TimeUnit.DAYS);
		} else if (period.getHours() > 0) {
			return FiniteDuration.create(period.getHours(), TimeUnit.HOURS);
		} else if (period.getMinutes() > 0) {
			return FiniteDuration.create(period.getMinutes(), TimeUnit.MINUTES);
		} else {
			throw new IllegalArgumentException("Unsupported period: " + period);
		}
	}

	public ImmutableList<Job> findJobs() {
		return jobs;
	}

	public void close() {
		for (Cancellable c : scheduled) {
			c.cancel();
		}
	}
}
