package com.zenobase.tasks.google;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class SessionsResultTest extends ResultTestSupport {

	@Test
	public void test() {

		SessionsResult result = new SessionsResult(readObject("SessionsResultTest.json"), TESTER, DateTimeZone.forID("America/Los_Angeles"));
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(2);
		assertThat(result.getNextPageToken()).isEqualTo("1414618922396");

		Event e1 = new Event(events.get(0).getId());
		e1.setValue(Event.TAG, "Walking");
		e1.setValue(Event.AUTHOR, TESTER);
		e1.setValue(Event.SOURCE, new Resource("Example App", "http://example.com/"));
		e1.addValue(Event.TIMESTAMP, dateTime("2014-10-28T12:22:00-07:00"));
		e1.addValue(Event.TIMESTAMP, dateTime("2014-10-28T12:47:00-07:00"));
		e1.setValue(Event.DURATION, Duration.standardMinutes(25));
		assertThat(events.get(0)).as("1st event").isEqualTo(e1);

		Event e2 = new Event(events.get(1).getId());
		e2.setValue(Event.AUTHOR, TESTER);
		e2.setValue(Event.SOURCE, new Resource("Strava", "https://www.strava.com/"));
		e2.addValue(Event.TIMESTAMP, dateTime("2014-10-29T13:00:00-07:00"));
		e2.addValue(Event.TIMESTAMP, dateTime("2014-10-29T13:36:00-07:00"));
		e2.setValue(Event.DURATION, Duration.standardMinutes(36));
		assertThat(events.get(1)).as("2nd event").isEqualTo(e2);
	}
}
