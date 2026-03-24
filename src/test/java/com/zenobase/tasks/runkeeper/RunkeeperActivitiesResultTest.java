package com.zenobase.tasks.runkeeper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.common.Pace;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class RunkeeperActivitiesResultTest extends ResultTestSupport {

	private final Identity author = new Identity();

	@Test
	public void test() {

		RunkeeperActivitiesResult result = new RunkeeperActivitiesResult(readObject("RunkeeperActivitiesResultTest.json"), author, Units.KM, Units.KCAL, DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.getNext()).isEqualTo("/fitnessActivities?page=1&pageSize=2");
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(2);

		Event e1 = new Event(events.get(0).getId());
		e1.addValue(Event.TAG, "Hiking");
		e1.setValue(Event.TIMESTAMP, dateTime("2013-11-09T11:50:48-08:00"));
		e1.setValue(Event.DURATION, Duration.millis(16121187L));
		e1.setValue(Event.DISTANCE, Measures.valueOf("6.16 km"));
		e1.setValue(Event.VELOCITY, Measures.valueOf("1.4 kmh"));
		e1.setValue(Event.PACE, Measures.valueOf("2617 s/km"));
		e1.setValue(Event.ENERGY, Measures.valueOf("1561 kcal"));
		e1.setValue(Event.SOURCE, new Resource("RunKeeper", "/fitnessActivities/268390846"));
		e1.setValue(Event.AUTHOR, author);
		assertThat(events.get(0)).isEqualTo(e1);

		Event e2 = new Event(events.get(1).getId());
		e2.addValue(Event.TAG, "Other");
		e2.setValue(Event.TIMESTAMP, dateTime("2014-11-06T11:48:07-07:30"));
		e2.setValue(Event.SOURCE, new Resource("RunKeeper", "/fitnessActivities/465890172"));
		e2.setValue(Event.AUTHOR, author);
		assertThat(events.get(1)).isEqualTo(e2);
	}

	@Test
	public void testMeters() {
		RunkeeperActivitiesResult result = new RunkeeperActivitiesResult(readObject("RunkeeperActivitiesResultTest.json"), author, Units.M, Units.KCAL, DateTimeZone.forID("America/Los_Angeles"));
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(2);
		assertThat(events.get(0).getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("6164.05 m"));
		assertThat(events.get(0).getValue(Event.VELOCITY)).isEqualTo(Measures.valueOf("1.4 kmh"));
		assertThat(events.get(0).getValue(Event.PACE)).isEqualTo(Measures.valueOf("2615 s/km"));
	}

	@Test
	public void testMiles() {
		Identity author = new Identity();
		RunkeeperActivitiesResult result = new RunkeeperActivitiesResult(readObject("RunkeeperActivitiesResultTest.json"), author, Units.MI, Units.KCAL, DateTimeZone.forID("America/Los_Angeles"));
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(2);
		assertThat(events.get(0).getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("3.83 mi"));
		assertThat(events.get(0).getValue(Event.VELOCITY)).isEqualTo(Measures.valueOf("0.9 mph"));
		assertThat(events.get(0).getValue(Event.PACE)).isEqualTo(Measures.valueOf("4209 s/mi"));
	}
}
