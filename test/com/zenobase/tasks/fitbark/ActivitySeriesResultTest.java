package com.zenobase.tasks.fitbark;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class ActivitySeriesResultTest extends ResultTestSupport {

	@Test
	public void testDaily() {
		List<Event> events = read("ActivitySeriesResultTest-Daily.json");
		assertThat(events).hasSize(1);
		Event expected = new Event(events.get(0).getId());
		expected.addValue(Event.TAG, "Jessie");
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, ActivitySeriesResult.SOURCE);
		expected.addValue(Event.TIMESTAMP, dateTime("2016-01-29T00:00:00-08:00"));
		expected.setValue(Event.DURATION, Duration.standardDays(1));
		expected.setValue(Event.COUNT, 379);
		assertThat(events.get(0)).isEqualTo(expected);
	}

	@Test
	public void testHourly() {
		List<Event> events = read("ActivitySeriesResultTest-Hourly.json");
		assertThat(events).hasSize(21);
		Event expected = new Event(events.get(19).getId());
		expected.addValue(Event.TAG, "Jessie");
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, ActivitySeriesResult.SOURCE);
		expected.addValue(Event.TIMESTAMP, dateTime("2016-01-29T09:00:00-08:00"));
		expected.setValue(Event.DURATION, Duration.standardHours(1));
		expected.setValue(Event.COUNT, 112);
		assertThat(events.get(19)).isEqualTo(expected);
	}

	private List<Event> read(String path) {
		return new ActivitySeriesResult("Jessie", TESTER, dateTime("2016-01-28T12:00:00-08:00"), DateTimeZone.forID("America/Los_Angeles"), readObject(path)).getEvents();
	}
}
