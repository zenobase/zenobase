package com.zenobase.tasks.foursquare;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class FoursquareResultTest extends ResultTestSupport {

	@Test
	public void test() {
		FoursquareResult result = new FoursquareResult(TESTER, readObject("FoursquareResultTest.json"));
		assertThat(result.getStatus()).as("status").isEqualTo(200);
		assertThat(result.getTotal()).as("total").isEqualTo(121);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(3);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, dateTime("2012-11-29T21:23:46-08:00"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, FoursquareResult.SOURCE);
		expected.setValue(Event.RESOURCE, new Resource("Queen Anne Pool", FoursquareResult.SOURCE.url()));
		expected.setValue(Event.LOCATION, new Location("47.636366468491374", "-122.35784366726875"));
		expected.addValue(Event.TAG, "Pool");
		expected.setValue(Event.NOTE, "20 laps");
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
