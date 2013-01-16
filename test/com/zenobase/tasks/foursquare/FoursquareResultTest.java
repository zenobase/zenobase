package com.zenobase.tasks.foursquare;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;
import com.zenobase.tasks.foursquare.FoursquareResult;


public class FoursquareResultTest extends ResultTestSupport {

	@Test
	public void test() {
		FoursquareResult result = new FoursquareResult(TESTER, readObject("FoursquareResultTest.json"));
		assertThat(result.getStatus()).as("status").isEqualTo(200);
		assertThat(result.getTotal()).as("total").isEqualTo(121);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(3);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, DateTime.parse("2012-11-29T21:23:46.000-08:00"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, FoursquareResult.SOURCE);
		expected.setValue(Event.RESOURCE, new Resource("Queen Anne Pool", FoursquareResult.SOURCE.getUrl()));
		expected.setValue(Event.LOCATION, new Location("47.636366468491374", "-122.35784366726875"));
		expected.addValue(Event.TAG, "Pool");
		expected.setValue(Event.NOTE, "20 laps");
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
