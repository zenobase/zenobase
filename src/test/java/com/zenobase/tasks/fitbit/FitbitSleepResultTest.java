package com.zenobase.tasks.fitbit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.jupiter.api.Test;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;

public class FitbitSleepResultTest extends ResultTestSupport {

	private static final String TAG = "zzz";

	@Test
	public void test() {
		FitbitSleepResult result = new FitbitSleepResult(
			readObject("FitbitSleepResultTest.json"),
			TAG,
			TESTER,
			true,
			DateTimeZone.forOffsetHours(-8)
		);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(1);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TAG, TAG);
		expected.addValue(Event.TIMESTAMP, dateTime("2012-11-28T00:58:00-08:00"));
		expected.addValue(Event.TIMESTAMP, dateTime("2012-11-28T08:48:00-08:00"));
		expected.setValue(Event.DURATION, Duration.standardMinutes(470));
		expected.setValue(Event.RATING, Rating.valueOf(100));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, FitbitSleepResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}

	@Test
	public void testEmpty() {
		FitbitSleepResult result = new FitbitSleepResult(
			Nodes.newObject(),
			TAG,
			TESTER,
			true,
			DateTimeZone.forOffsetHours(-8)
		);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(0);
	}
}
