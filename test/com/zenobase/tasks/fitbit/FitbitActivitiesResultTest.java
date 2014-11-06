package com.zenobase.tasks.fitbit;

import static org.fest.assertions.Assertions.assertThat;

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
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class FitbitActivitiesResultTest extends ResultTestSupport {

	@Test
	public void test() {

		FitbitActivitiesResult result = new FitbitActivitiesResult(readObject("FitbitActivitiesResultTest.json"), TESTER, DateTimeZone.forID("America/Los_Angeles"), Units.MI);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(2);

		Event e1 = new Event(events.get(0).getId());
		e1.setValue(Event.TAG, "Walk");
		e1.setValue(Event.TIMESTAMP, dateTime("2014-10-28T18:08:00-07:00"));
		e1.setValue(Event.DURATION, Duration.standardSeconds(2079));
		e1.setValue(Event.DISTANCE, Measures.<Length>valueOf("3.72 mi"));
		e1.setValue(Event.VELOCITY, Measures.<Velocity>valueOf("6.4 mph"));
		e1.setValue(Event.PACE, Measures.<Pace>valueOf("559 s/mi"));
		e1.setValue(Event.ENERGY, Measures.<Energy>valueOf("240 kcal"));
		e1.setValue(Event.COUNT, 4848);
		e1.setValue(Event.AUTHOR, TESTER);
		e1.setValue(Event.SOURCE, FitbitSleepResult.SOURCE);
		assertThat(events.get(0)).isEqualTo(e1);

		Event e2 = new Event(events.get(1).getId());
		e2.setValue(Event.TAG, "Dynaflex");
		e2.setValue(Event.TIMESTAMP, dateTime("2014-10-28T12:22:00-07:00"));
		e2.setValue(Event.DURATION, Duration.standardMinutes(5));
		e2.setValue(Event.ENERGY, Measures.<Energy>valueOf("105 kcal"));
		e2.setValue(Event.AUTHOR, TESTER);
		e2.setValue(Event.SOURCE, FitbitSleepResult.SOURCE);
		assertThat(events.get(1)).isEqualTo(e2);

	}

	@Test
	public void testEmpty() {
		FitbitActivitiesResult result = new FitbitActivitiesResult(Nodes.newObject(), TESTER, DateTimeZone.forID("America/Los_Angeles"), Units.MI);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(0);
	}
}
