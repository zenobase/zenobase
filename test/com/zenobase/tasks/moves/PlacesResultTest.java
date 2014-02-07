package com.zenobase.tasks.moves;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.tasks.ResultTestSupport;

public class PlacesResultTest extends ResultTestSupport {

	@Test
	public void test() {
		DateTime begin = DateTime.parse("20121212T074617+0200", ISODateTimeFormat.basicDateTimeNoMillis().withOffsetParsed());
		PlacesResult result = new PlacesResult(TESTER, begin, readArray("PlacesResultTest.json"));
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(4);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, begin);
		expected.addValue(Event.TAG, "Place");
		expected.setValue(Event.LOCATION, new Location("55.55555", "33.33333"));
		expected.setValue(Event.DURATION, Duration.standardSeconds(8074));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, PlacesResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
