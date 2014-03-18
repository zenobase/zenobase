package com.zenobase.tasks.withings;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class WithingsSleepResultTest extends ResultTestSupport {

	@Test
	public void test() {
		WithingsSleepResult result = new WithingsSleepResult(readObject("WithingsSleepResultTest.json"), TESTER, "sleep", DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.getStatus()).as("status").isEqualTo(0);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(6);
		events = WithingsSleepResult.merge(result.getEvents());
		assertThat(events).as("events").hasSize(2);
		checkFirst(events.get(0));
		checkLast(events.get(1));
	}

	private void checkFirst(Event event) {
		Event expected = new Event(event.getId());
		expected.setValue(Event.TAG, "sleep");
		expected.setValue(Event.TIMESTAMP, DateTime.parse("2014-03-11T01:39:26.000-07:00"));
		expected.setValue(Event.DURATION, Duration.standardSeconds(25500));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, WithingsWeightResult.SOURCE);
		assertThat(event).as("first event").isEqualTo(expected);
	}

	private void checkLast(Event event) {
		Event expected = new Event(event.getId());
		expected.setValue(Event.TAG, "sleep");
		expected.setValue(Event.TIMESTAMP, DateTime.parse("2014-03-12T03:35:17.000-07:00"));
		expected.setValue(Event.DURATION, Duration.standardSeconds(26880));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, WithingsWeightResult.SOURCE);
		assertThat(event).as("last event").isEqualTo(expected);
	}
}
