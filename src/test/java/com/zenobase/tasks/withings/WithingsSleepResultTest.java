package com.zenobase.tasks.withings;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.jupiter.api.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.tasks.ResultTestSupport;

public class WithingsSleepResultTest extends ResultTestSupport {

	@Test
	public void test() {
		WithingsSleepResult result = new WithingsSleepResult(
				readObject("WithingsSleepResultTest.json"),
				TESTER,
				"sleep",
				true,
				DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.getStatus()).as("status").isEqualTo(0);
		assertThat(result.getEvents()).as("events").hasSize(6);
		WithingsSleepResult merged = result.merge();
		assertThat(merged.getEvents()).as("events").hasSize(2);
		checkFirst(merged.getEvents().get(0));
		checkLast(merged.getEvents().get(1));
		assertThat(merged.getMarker()).as("marker").isEqualTo("2014-03-12T11:03:17.000-07:00");
	}

	private void checkFirst(Event event) {
		Event expected = new Event(event.getId());
		expected.setValue(Event.TAG, "sleep");
		expected.addValue(Event.TIMESTAMP, dateTime("2014-03-11T01:39:26-07:00"));
		expected.addValue(Event.TIMESTAMP, dateTime("2014-03-11T08:44:26-07:00"));
		expected.setValue(Event.DURATION, Duration.standardSeconds(25500));
		expected.setValue(Event.PERCENTAGE, Percentage.valueOf(98));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, WithingsWeightResult.SOURCE);
		assertThat(event).as("first event").isEqualTo(expected);
	}

	private void checkLast(Event event) {
		Event expected = new Event(event.getId());
		expected.setValue(Event.TAG, "sleep");
		expected.addValue(Event.TIMESTAMP, dateTime("2014-03-12T03:35:17-07:00"));
		expected.addValue(Event.TIMESTAMP, dateTime("2014-03-12T11:03:17-07:00"));
		expected.setValue(Event.DURATION, Duration.standardSeconds(26880));
		expected.setValue(Event.PERCENTAGE, Percentage.valueOf(96));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, WithingsWeightResult.SOURCE);
		assertThat(event).as("last event").isEqualTo(expected);
	}
}
