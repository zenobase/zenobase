package com.zenobase.tasks.fitbit;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.common.Pace;
import com.zenobase.common.Units;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class FitbitActivitiesResultTest extends ResultTestSupport {

	@Test
	public void test() {

		FitbitActivitiesResult result = new FitbitActivitiesResult(readObject("FitbitActivitiesResultTest.json"), TESTER, true, Units.MI);
		assertThat(result.next()).isNotNull();
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(2);

		Event e1 = new Event(events.get(0).getId());
		e1.setValue(Event.TAG, "Hike");
		e1.setValue(Event.TIMESTAMP, dateTime("2015-05-25T14:08:12-07:00"));
		e1.setValue(Event.DURATION, Duration.standardSeconds(7272));
		e1.setValue(Event.DISTANCE, Measures.<Length>valueOf("5.600688 mi"));
		e1.setValue(Event.VELOCITY, Measures.<Velocity>valueOf("3.24 mph"));
		e1.setValue(Event.PACE, Measures.<Pace>valueOf("1111 s/mi"));
		e1.setValue(Event.ENERGY, Measures.<Energy>valueOf("582 kcal"));
		e1.setValue(Event.FREQUENCY, Measures.<Frequency>valueOf("102 bpm"));
		e1.setValue(Event.COUNT, 7295);
		e1.setValue(Event.AUTHOR, TESTER);
		e1.setValue(Event.SOURCE, FitbitActivitiesResult.SOURCE);
		assertThat(events.get(0)).isEqualTo(e1);

		Event e2 = new Event(events.get(1).getId());
		e2.setValue(Event.TAG, "Walk");
		e2.setValue(Event.TIMESTAMP, dateTime("2016-03-26T14:49:05-07:00"));
		e2.setValue(Event.DURATION, Duration.standardSeconds(1503));
		e2.setValue(Event.ENERGY, Measures.<Energy>valueOf("134 kcal"));
		e2.setValue(Event.FREQUENCY, Measures.<Frequency>valueOf("96 bpm"));
		e2.setValue(Event.COUNT, 1662);
		e2.setValue(Event.AUTHOR, TESTER);
		e2.setValue(Event.SOURCE, FitbitActivitiesResult.SOURCE);
		assertThat(events.get(1)).isEqualTo(e2);
	}

	@Test
	public void testExcludeAutodetected() {
		FitbitActivitiesResult result = new FitbitActivitiesResult(readObject("FitbitActivitiesResultTest.json"), TESTER, false, Units.MI);
		assertThat(result.next()).isNotNull();
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(1);
		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo("Hike");
	}

	@Test
	public void testEmpty() {
		FitbitActivitiesLegacyResult result = new FitbitActivitiesLegacyResult(Nodes.newObject(), TESTER, DateTimeZone.forID("America/Los_Angeles"), Units.MI);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(0);
	}
}
