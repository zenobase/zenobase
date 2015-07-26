package com.zenobase.tasks.microsoft;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

public class StepsResultTest extends ResultTestSupport {

	@Test
	public void test() {
		StepsResult result = new StepsResult(readObject("StepsResultTest.json"), TESTER, DateTimeZone.forID("Europe/Berlin"), "Day", true);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(2);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, dateTime("2015-07-26T00:00:00+02:00"));
		expected.addValue(Event.TAG, "Day");
		expected.setValue(Event.DURATION, Duration.standardDays(1));
		expected.setValue(Event.DISTANCE, Measures.<Length>valueOf("1.86 km"));
		expected.setValue(Event.FREQUENCY, Measures.<Frequency>valueOf("68 bpm"));
		expected.setValue(Event.ENERGY, Measures.<Energy>valueOf("501 kcal"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, StepsResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}

	@Test
	public void testImperialUnits() {
		StepsResult result = new StepsResult(readObject("StepsResultTest.json"), TESTER, DateTimeZone.forID("Europe/Berlin"), "Day", false);
		List<Event> events = result.getEvents();
		assertThat(events.get(0).getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("1.15 mi"));
	}
}
