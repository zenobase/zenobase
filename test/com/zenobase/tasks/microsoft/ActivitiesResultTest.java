package com.zenobase.tasks.microsoft;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

import org.joda.time.Duration;
import org.junit.Test;

public class ActivitiesResultTest extends ResultTestSupport {

	@Test
	public void test() {
		ActivitiesResult result = new ActivitiesResult(readObject("ActivitiesResultTest.json"), TESTER, true);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(6);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, dateTime("2015-07-11T17:42:57Z"));
		expected.addValue(Event.TAG, "Run");
		expected.setValue(Event.DURATION, Duration.standardSeconds(3303));
		expected.setValue(Event.DISTANCE, Measures.<Length>valueOf("2.83 km"));
		expected.setValue(Event.FREQUENCY, Measures.<Frequency>valueOf("81 bpm"));
		expected.setValue(Event.ENERGY, Measures.<Energy>valueOf("175 kcal"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, ActivitiesResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
