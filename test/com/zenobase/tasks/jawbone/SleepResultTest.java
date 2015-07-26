package com.zenobase.tasks.jawbone;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;

public class SleepResultTest extends ResultTestSupport {

	@Test
	public void test() {
		SleepResult result = new SleepResult(readObject("MicrosoftHealthSleepResultTest.json"), TESTER, "sleep", true);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(10);
		Event expected = new Event(events.get(0).getId());
		expected.addValue(Event.TIMESTAMP, dateTime("2014-02-28T22:00:00-07:00"));
		expected.addValue(Event.TIMESTAMP, dateTime("2014-02-28T22:13:19-07:00"));
		expected.setValue(Event.DURATION, Duration.standardSeconds(799));
		expected.addValue(Event.TAG, "sleep");
		expected.setValue(Event.RATING, Rating.valueOf(90));
		expected.setValue(Event.LOCATION, new Location("47.6097", "-122.3331"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, JawboneResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
