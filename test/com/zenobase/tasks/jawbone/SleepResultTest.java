package com.zenobase.tasks.jawbone;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;

public class SleepResultTest extends ResultTestSupport {

	@Test
	public void test() {
		DateTime begin = DateTime.parse("20140228T220000-07:00", ISODateTimeFormat.basicDateTimeNoMillis().withOffsetParsed());
		SleepResult result = new SleepResult(readObject("SleepResultTest.json"), TESTER, "sleep");
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(10);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, begin);
		expected.setValue(Event.DURATION, Duration.standardSeconds(799));
		expected.addValue(Event.TAG, "sleep");
		expected.setValue(Event.RATING, Rating.valueOf(90));
		expected.setValue(Event.LOCATION, new Location("47.6097", "-122.3331"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, JawboneResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
