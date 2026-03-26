package com.zenobase.tasks.fitbit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.Duration;
import org.junit.jupiter.api.Test;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class FitbitActivitiesResultTest extends ResultTestSupport {

	@Test
	public void test() {

		FitbitActivitiesResult result =
				new FitbitActivitiesResult(readObject("FitbitActivitiesResultTest.json"), TESTER, true, Units.MI);
		assertThat(result.next()).isNotNull();
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(2);

		Event e1 = new Event(events.get(0).getId());
		e1.setValue(Event.TAG, "Hike");
		e1.setValue(Event.TIMESTAMP, dateTime("2015-05-25T14:08:12-07:00"));
		e1.setValue(Event.DURATION, Duration.standardSeconds(7272));
		e1.setValue(Event.DISTANCE, Measures.valueOf("5.600688 mi"));
		e1.setValue(Event.VELOCITY, Measures.valueOf("3.24 mph"));
		e1.setValue(Event.PACE, Measures.valueOf("1111 s/mi"));
		e1.setValue(Event.ENERGY, Measures.valueOf("582 kcal"));
		e1.setValue(Event.FREQUENCY, Measures.valueOf("102 bpm"));
		e1.setValue(Event.COUNT, 7295);
		e1.setValue(Event.AUTHOR, TESTER);
		e1.setValue(Event.SOURCE, FitbitActivitiesResult.SOURCE);
		assertThat(events.get(0)).isEqualTo(e1);

		Event e2 = new Event(events.get(1).getId());
		e2.setValue(Event.TAG, "Walk");
		e2.setValue(Event.TIMESTAMP, dateTime("2016-03-26T14:49:05-07:00"));
		e2.setValue(Event.DURATION, Duration.standardSeconds(1503));
		e2.setValue(Event.ENERGY, Measures.valueOf("134 kcal"));
		e2.setValue(Event.FREQUENCY, Measures.valueOf("96 bpm"));
		e2.setValue(Event.COUNT, 1662);
		e2.setValue(Event.AUTHOR, TESTER);
		e2.setValue(Event.SOURCE, FitbitActivitiesResult.SOURCE);
		assertThat(events.get(1)).isEqualTo(e2);
	}

	@Test
	public void testExcludeAutodetected() {
		FitbitActivitiesResult result =
				new FitbitActivitiesResult(readObject("FitbitActivitiesResultTest.json"), TESTER, false, Units.MI);
		assertThat(result.next()).isNotNull();
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(1);
		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo("Hike");
	}

	@Test
	public void testEmpty() {
		FitbitActivitiesResult result = new FitbitActivitiesResult(Nodes.newObject(), TESTER, false, Units.MI);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(0);
	}
}
