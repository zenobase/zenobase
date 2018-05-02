package com.zenobase.tasks.jawbone;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;

import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.tasks.ResultTestSupport;

public class WorkoutsResultTest extends ResultTestSupport {

	@Test
	public void test() {
		WorkoutsResult result = new WorkoutsResult(readObject("WorkoutsResultTest.json"), TESTER, true);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(3);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, dateTime("2013-04-14T06:46:47-07:00"));
		expected.setValue(Event.DURATION, Duration.standardSeconds(8321));
		expected.addValue(Event.TAG, "walk");
		expected.setValue(Event.LOCATION, new Location("47.6097", "-122.3331"));
		expected.setValue(Event.COUNT, 47);
		expected.setValue(Event.DISTANCE, Measures.<Length>valueOf("0.04 km"));
		expected.setValue(Event.ENERGY, Measures.<Energy>valueOf("1.88942942023 kcal"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, JawboneResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
