package com.zenobase.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.zenobase.services.Bus;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

public class SchedulerTest {

	private final Bus bus = mock(Bus.class);
	private final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

	@BeforeEach
	public void freezeClock() {
		DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2014-12-13T17:50:00Z").getMillis());
	}

	@AfterEach
	public void resetClock() {
		DateTimeUtils.setCurrentMillisSystem();
	}

	// Clock frozen at 17:50:00 UTC.
	@ParameterizedTest
	@CsvSource({
		"00:00, PT5M,  PT0S,  PT5M",
		"00:01, PT5M,  PT1M,  PT5M",
		"18:00, PT12H, PT10M, PT12H",
		"17:50, P1D,   PT0S,  P1D",
	})
	public void testInitialDelayAndPeriod(
		String begin,
		String period,
		String expectedInitialDelay,
		String expectedPeriod
	) {
		new Scheduler(bus, Set.of(new FakeJob(LocalTime.parse(begin), Period.parse(period))), executor).start();
		verify(executor).scheduleAtFixedRate(
			any(Runnable.class),
			eq(Period.parse(expectedInitialDelay).toStandardDuration().getMillis()),
			eq(Period.parse(expectedPeriod).toStandardDuration().getMillis()),
			eq(TimeUnit.MILLISECONDS)
		);
	}

	@Test
	public void testScheduledRunnableInvokesJob() {
		var job = new FakeJob();
		captureScheduledRunnable(job).run();
		assertThat(job.ran).isTrue();
	}

	@Test
	public void testScheduledRunnableSkipsWhenReadOnly() {
		var job = new FakeJob();
		when(bus.isReadOnly()).thenReturn(true);
		captureScheduledRunnable(job).run();
		assertThat(job.ran).isFalse();
	}

	@Test
	public void testScheduledRunnableSkipsWhenSchedulerDisabled() {
		var job = new FakeJob();
		when(bus.isSchedulerDisabled()).thenReturn(true);
		captureScheduledRunnable(job).run();
		assertThat(job.ran).isFalse();
	}

	@Test
	public void testScheduledRunnableSwallowsJobExceptions() {
		var throwing = mock(Job.class);
		when(throwing.getLabel()).thenReturn("throwing");
		when(throwing.getBegin()).thenReturn(new LocalTime(2, 0));
		when(throwing.getPeriod()).thenReturn(Period.hours(6));
		doThrow(new RuntimeException("boom")).when(throwing).run();

		captureScheduledRunnable(throwing).run();
		verify(throwing).run();
	}

	@Test
	public void testCloseShutsDownExecutor() {
		new Scheduler(bus, Set.of(), executor).close();
		verify(executor).shutdownNow();
	}

	private Runnable captureScheduledRunnable(Job job) {
		var scheduler = new Scheduler(bus, Set.of(job), executor);
		scheduler.start();
		var captor = ArgumentCaptor.forClass(Runnable.class);
		verify(executor).scheduleAtFixedRate(captor.capture(), anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS));
		return captor.getValue();
	}

	private static class FakeJob extends Job {

		boolean ran;

		FakeJob() {
			this(new LocalTime(2, 0), Period.hours(6));
		}

		FakeJob(LocalTime begin, Period period) {
			super("fake", begin, period);
		}

		@Override
		public void run() {
			ran = true;
		}
	}
}
