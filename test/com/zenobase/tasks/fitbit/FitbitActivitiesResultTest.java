package com.zenobase.tasks.fitbit;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class FitbitActivitiesResultTest extends ResultTestSupport {

	@Test
	public void test() {
		FitbitActivitiesResult result = new FitbitActivitiesResult(readObject("FitbitActivitiesResultTest.json"), TESTER, DateTimeZone.forID("America/Los_Angeles"), Units.MI);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(2);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TAG, "Walk");
		expected.setValue(Event.TIMESTAMP, dateTime("2014-10-28T18:08:00-07:00"));
		expected.setValue(Event.DURATION, Duration.standardSeconds(2079));
		expected.setValue(Event.DISTANCE, Measures.<Length>valueOf("3.72 mi"));
		expected.setValue(Event.ENERGY, Measures.<Energy>valueOf("240 kcal"));
		expected.setValue(Event.COUNT, 4848);
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, FitbitSleepResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}

	@Test
	public void testEmpty() {
		FitbitActivitiesResult result = new FitbitActivitiesResult(Nodes.newObject(), TESTER, DateTimeZone.forID("America/Los_Angeles"), Units.MI);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(0);
	}
}
