package com.zenobase.tasks.mapmyfitness;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.tasks.ResultTestSupport;

import org.joda.time.Duration;
import org.junit.Test;

public class SleepResultTest extends ResultTestSupport {

	@Test
	public void test() {

		SleepResult result = new SleepResult(readObject("SleepResultTest.json"), TESTER, "sleep");
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);

		Event actual = events.get(0);
		Event expected = new Event(actual.getId());
		expected.addValue(Event.TAG, "sleep");
		expected.addValue(Event.TIMESTAMP, dateTime("2015-01-06T23:30:00-08:00"));
		expected.addValue(Event.TIMESTAMP, dateTime("2015-01-07T05:30:00-08:00"));
		expected.setValue(Event.DURATION, Duration.standardHours(6));
		expected.setValue(Event.PERCENTAGE, Percentage.valueOf(90));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, SleepResult.SOURCE);
		assertThat(actual).isEqualTo(expected);
	}
}
