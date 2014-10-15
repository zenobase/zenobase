package com.zenobase.tasks.bodymedia;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.ResultTestSupport;

public class BodyMediaStepsResultTest extends ResultTestSupport {

	private static final String TAG = "my steps";

	private final TimezoneMap timezones = new TimezoneMap();
	private final Identity author = new Identity();

	@Test
	public void testGainOneHour() {
		addTimezone("2013-01-01T00:00:00-08:00", "America/Los_Angeles");
		BodyMediaStepsResult result = parse("BodyMediaStepsResultTest-gainOneHour.json", true);
		List<Event> events = result.getEvents();
		assertThat(result.getDate()).isEqualTo(LocalDate.parse("2013-11-03"));
		assertThat(events).hasSize(25);
		assertEvent(events.get(0), "2013-11-03T00:00:00-0700", 91, 1);
		assertEvent(events.get(1), "2013-11-03T01:00:00-0700", 3, 1);
		assertEvent(events.get(2), "2013-11-03T01:00:00-0800", 2, 1);
		assertEvent(events.get(3), "2013-11-03T02:00:00-0800", 1, 1);
		assertEvent(events.get(24), "2013-11-03T23:00:00-0800", 46, 1);
	}

	@Test
	public void testGainOneHourDaily() {
		addTimezone("2013-01-01T00:00:00-08:00", "America/Los_Angeles");
		BodyMediaStepsResult result = parse("BodyMediaStepsResultTest-gainOneHour.json", false);
		List<Event> events = result.getEvents();
		assertThat(result.getDate()).isEqualTo(LocalDate.parse("2013-11-03"));
		assertThat(events).hasSize(1);
		assertEvent(events.get(0), "2013-11-03T00:00:00-0700", 8928, 25);
	}

	@Test
	public void testLoseOneHour() {
		addTimezone("2013-01-01T00:00:00-08:00", "America/Los_Angeles");
		BodyMediaStepsResult result = parse("BodyMediaStepsResultTest-loseOneHour.json", true);
		List<Event> events = result.getEvents();
		assertThat(result.getDate()).isEqualTo(LocalDate.parse("2013-03-10"));
		assertThat(events).hasSize(23);
		assertEvent(events.get(0), "2013-03-10T00:00:00-0800", 23, 1);
		assertEvent(events.get(1), "2013-03-10T01:00:00-0800", 22, 1);
		assertEvent(events.get(2), "2013-03-10T03:00:00-0700", 5, 1);
		assertEvent(events.get(3), "2013-03-10T04:00:00-0700", 4, 1);
		assertEvent(events.get(22), "2013-03-10T23:00:00-0700", 39, 1);
	}

	@Test
	public void testNoData() {
		addTimezone("2013-01-01T00:00:00-08:00", "America/Los_Angeles");
		BodyMediaStepsResult result = parse("BodyMediaStepsResultTest-empty.json", true);
		List<Event> events = result.getEvents();
		assertThat(result.getDate()).isEqualTo(LocalDate.parse("2013-11-03"));
		assertThat(events).isEmpty();
	}

	private void addTimezone(String from, String timezone) {
		timezones.add(DateTime.parse(from), null, DateTimeZone.forID(timezone));
	}

	private BodyMediaStepsResult parse(String source, boolean hourly) {
		return new BodyMediaStepsResult(readObject(source), author, TAG, hourly, timezones);
	}

	private static void assertEvent(Event event, String timestamp, int steps, int hours) {
		assertThat(event.getValue(Event.TAG)).isEqualTo(TAG);
		assertThat(event.getValue(Event.TIMESTAMP)).isEqualTo(DateTime.parse(timestamp));
		assertThat(event.getValue(Event.DURATION)).isEqualTo(Duration.standardHours(hours));
		assertThat(event.getValue(Event.COUNT)).isEqualTo(steps);
	}
}
