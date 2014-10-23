package com.zenobase.tasks.fitbit;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.junit.Test;
import com.google.common.collect.Lists;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class FitbitIntradayResultTest extends ResultTestSupport {

	private static final DateTimeZone TIMEZONE = DateTimeZone.forOffsetHours(-8);
	private static final LocalDate DATE = LocalDate.parse("2012-12-03");

	@Test
	public void test() {
		List<Interval> ignore = Lists.newArrayList(
			new Interval(DateTime.parse("2012-12-03T00:30:05.000-08:00"), DateTime.parse("2012-12-03T08:25:00.000-08:00")),
			new Interval(DateTime.parse("2012-12-03T23:00:00.000-08:00"), DateTime.parse("2012-12-04T08:00:00.000-08:00"))
		);
		FitbitIntradayResult result = new FitbitIntradayResult(readObject("FitbitIntradayResultTest.json"), TESTER, DATE, TIMEZONE, ignore);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(82);
		Event first = new Event(events.get(0).getId());
		first.setValue(Event.TAG, "sitting");
		first.setValue(Event.TIMESTAMP, DateTime.parse("2012-12-03T00:00:00.000-08:00"));
		first.setValue(Event.DURATION, Duration.millis(1920000));
		first.setValue(Event.AUTHOR, TESTER);
		first.setValue(Event.SOURCE, FitbitIntradayResult.SOURCE);
		Event second = new Event(events.get(1).getId());
		second.setValue(Event.TAG, "sitting");
		second.setValue(Event.TIMESTAMP, DateTime.parse("2012-12-03T08:25:00.000-08:00"));
		second.setValue(Event.DURATION, Duration.millis(1680000));
		second.setValue(Event.AUTHOR, TESTER);
		second.setValue(Event.SOURCE, FitbitIntradayResult.SOURCE);
		assertThat(events.get(1)).as("second event").isEqualTo(second);
	}

	@Test
	public void testEmpty() {
		FitbitIntradayResult result = new FitbitIntradayResult(Nodes.newObject(), TESTER, DATE, TIMEZONE, Lists.<Interval>newArrayList());
		List<Event> events = result.getEvents();
		assertThat(events).as("events").isEmpty();
	}
}
