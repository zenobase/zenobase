package com.zenobase.tasks.bodymedia;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.ResultTestSupport;

public class BodyMediaBurnResultTest extends ResultTestSupport {

	private static final String TAG = "my burn";

	private final TimezoneMap timezones = new TimezoneMap();
	private final Identity author = new Identity();

	@Test
	public void testGainOneHour() {
		addTimezone("2013-01-01T00:00:00-08:00", "America/Los_Angeles");
		BodyMediaBurnResult result = parse("BodyMediaBurnResultTest-gainOneHour.json", true);
		List<Event> events = result.getEvents();
		assertThat(result.getDate()).isEqualTo(LocalDate.parse("2013-11-03"));
		assertThat(events).hasSize(25);
		assertEvent(events.get(0), "2013-11-03T00:00:00-0700", "98.082 kcal", 1);
		assertEvent(events.get(1), "2013-11-03T01:00:00-0700", "81.673 kcal", 1);
		assertEvent(events.get(2), "2013-11-03T01:00:00-0800", "78.222 kcal", 1);
		assertEvent(events.get(3), "2013-11-03T02:00:00-0800", "80.749 kcal", 1);
	}

	@Test
	public void testGainOneHourDaily() {
		addTimezone("2013-01-01T00:00:00-08:00", "America/Los_Angeles");
		BodyMediaBurnResult result = parse("BodyMediaBurnResultTest-gainOneHour.json", false);
		List<Event> events = result.getEvents();
		assertThat(result.getDate()).isEqualTo(LocalDate.parse("2013-11-03"));
		assertThat(events).hasSize(1);
		assertEvent(events.get(0), "2013-11-03T00:00:00-0700", "3498 kcal", 25);
	}

	@Test
	public void testLoseOneHour() {
		addTimezone("2013-01-01T00:00:00-08:00", "America/Los_Angeles");
		BodyMediaBurnResult result = parse("BodyMediaBurnResultTest-loseOneHour.json", true);
		List<Event> events = result.getEvents();
		assertThat(result.getDate()).isEqualTo(LocalDate.parse("2013-03-10"));
		assertThat(events).hasSize(23);
		assertEvent(events.get(0), "2013-03-10T00:00:00-0800", "98.082 kcal", 1);
		assertEvent(events.get(1), "2013-03-10T01:00:00-0800", "81.673 kcal", 1);
		assertEvent(events.get(2), "2013-03-10T03:00:00-0700", "78.222 kcal", 1);
		assertEvent(events.get(3), "2013-03-10T04:00:00-0700", "80.749 kcal", 1);
	}

	private void addTimezone(String from, String timezone) {
		timezones.add(DateTime.parse(from), null, DateTimeZone.forID(timezone));
	}

	private BodyMediaBurnResult parse(String source, boolean hourly) {
		return new BodyMediaBurnResult(readObject(source), author, TAG, hourly, Units.KCAL, timezones);
	}

	private static void assertEvent(Event event, String timestamp, String calories, int hours) {
		assertThat(event.getValue(Event.TAG)).isEqualTo(TAG);
		assertThat(event.getValue(Event.TIMESTAMP)).isEqualTo(DateTime.parse(timestamp));
		assertThat(event.getValue(Event.DURATION)).isEqualTo(Duration.standardHours(hours));
		assertThat(event.getValue(Event.ENERGY)).isEqualTo(Measures.<Energy>valueOf(calories));
	}
}
