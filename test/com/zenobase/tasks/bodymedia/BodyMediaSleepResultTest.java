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
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;

public class BodyMediaSleepResultTest extends ResultTestSupport {

	private static final String TAG = "my sleep";

	private final TimezoneMap timezones = new TimezoneMap();
	private final Identity author = new Identity();

	@Test
	public void testGainOneHour() {
		addTimezone("2013-01-01T00:00:00-08:00", "America/Los_Angeles");
		BodyMediaSleepResult result = parse("BodyMediaSleepResultTest-gainOneHour.json");
		List<Event> events = result.getEvents();
		assertThat(result.getDate()).isEqualTo(LocalDate.parse("2013-11-03"));
		assertThat(events).hasSize(2);
		assertEvent(events.get(0), "2013-11-03T00:14:00-0700", "2013-11-03T08:08:00-0800", 534, 79);
		assertEvent(events.get(1), "2013-11-03T08:13:00-0800", "2013-11-03T08:48:00-0800", 35, 6);
	}

	@Test
	public void testLoseOneHour() {
		addTimezone("2013-01-01T00:00:00-08:00", "America/Los_Angeles");
		BodyMediaSleepResult result = parse("BodyMediaSleepResultTest-loseOneHour.json");
		List<Event> events = result.getEvents();
		assertThat(result.getDate()).isEqualTo(LocalDate.parse("2013-03-10"));
		assertThat(events).hasSize(1);
		assertEvent(events.get(0), "2013-03-09T23:58:00-08:00", "2013-03-10T09:04:00-0700", 486, 84);
	}

	private void addTimezone(String from, String timezone) {
		timezones.add(DateTime.parse(from), null, DateTimeZone.forID(timezone));
	}

	private BodyMediaSleepResult parse(String source) {
		return new BodyMediaSleepResult(readObject(source), author, TAG, true, timezones);
	}

	private static void assertEvent(Event event, String begin, String end, int minutes, int rating) {
		assertThat(event.getValue(Event.TAG)).isEqualTo(TAG);
		assertThat(event.getValues(Event.TIMESTAMP)).containsExactly(dateTime(begin), dateTime(end));
		assertThat(event.getValue(Event.DURATION)).isEqualTo(Duration.standardMinutes(minutes));
		assertThat(event.getValue(Event.RATING)).isEqualTo(Rating.valueOf(rating));
	}
}
