package com.zenobase.tasks.fitbit;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;
import com.zenobase.tasks.fitbit.FitbitSleepResult;

public class FitbitSleepResultTest extends ResultTestSupport {

	@Test
	public void test() {
		FitbitSleepResult result = new FitbitSleepResult(readObject("FitbitSleepResultTest.json"), TESTER, DateTimeZone.forOffsetHours(-8));
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(1);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TAG, "sleeping");
		expected.setValue(Event.TIMESTAMP, DateTime.parse("2012-11-28T00:58:00.000-08:00"));
		expected.setValue(Event.DURATION, new Duration(28200000));
		expected.setValue(Event.RATING, Rating.valueOf(100));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, FitbitSleepResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}

	@Test
	public void testEmpty() {
		FitbitSleepResult result = new FitbitSleepResult(Nodes.newObject(), TESTER, DateTimeZone.forOffsetHours(-8));
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(0);
	}
}
