package com.zenobase.tasks.jawbone;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class StepsResultTest extends ResultTestSupport {

	@Test
	public void testDailyMetric() {
		DateTime begin = DateTime.parse("20140311T090000-07:00", ISODateTimeFormat.basicDateTimeNoMillis().withOffsetParsed());
		StepsResult result = new StepsResult(readObject("StepsResultTest.json"), TESTER, "steps", false, true);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(3);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, begin);
		expected.setValue(Event.DURATION, Duration.standardMinutes(719));
		expected.addValue(Event.TAG, "steps");
		expected.setValue(Event.COUNT, 16071);
		expected.setValue(Event.DISTANCE, Measures.<Length>valueOf("12.57 km"));
		expected.setValue(Event.ENERGY, Measures.<Energy>valueOf("1440 kcal"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, JawboneResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}

	@Test
	public void testDailyImperial() {
		StepsResult result = new StepsResult(readObject("StepsResultTest.json"), TESTER, "steps", false, false);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(3);
		assertThat(events.get(0).getValue(Event.DISTANCE)).isEqualTo(Measures.<Length>valueOf("7.81 mi"));
	}
}
