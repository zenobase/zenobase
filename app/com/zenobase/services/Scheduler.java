package com.zenobase.services;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import play.libs.Akka;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.Cancellable;
import com.google.common.collect.Lists;

public class Scheduler {

	private final List<Cancellable> scheduled = Lists.newArrayList();

	@Inject
	public Scheduler(final Bus bus, final IndexManager manager) {
		schedule(new LocalTime(1, 0), Period.hours(8), new Runnable() {
			@Override
			public void run() {
				if (bus.isMaster() && !bus.isReadOnly()) {
					manager.getSnapshotManager().snapshot();
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
		if (period.getHours() > 0) {
			return FiniteDuration.create(period.getHours(), TimeUnit.HOURS);
		} else if (period.getMinutes() > 0) {
			return FiniteDuration.create(period.getMillis(), TimeUnit.MINUTES);
		} else {
			throw new IllegalArgumentException("Unsupported period: " + period);
		}
	}

	public void close() {
		for (Cancellable c : scheduled) {
			c.cancel();
		}
	}
}
