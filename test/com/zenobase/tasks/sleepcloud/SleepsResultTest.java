package com.zenobase.tasks.sleepcloud;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;

public class SleepsResultTest extends ResultTestSupport {

	@Test
	public void test() {

		SleepsResult result = new SleepsResult("sleep", TESTER, true, readObject("SleepsResultTest.json"));
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(3);

		Event e1 = new Event(events.get(0).getId());
		e1.addValue(Event.TAG, "sleep");
		e1.addValue(Event.TAG, "home");
		e1.addValue(Event.TAG, "drink");
		e1.setValue(Event.AUTHOR, TESTER);
		e1.setValue(Event.SOURCE, SleepsResult.SOURCE);
		e1.addValue(Event.TIMESTAMP, dateTime("2014-03-09T22:09:26-05:00"));
		e1.addValue(Event.TIMESTAMP, dateTime("2014-03-10T06:26:50-05:00"));
		e1.setValue(Event.DURATION, Duration.millis(29844000L));
		e1.setValue(Event.COUNT, 8);
		e1.setValue(Event.PERCENTAGE, Percentage.valueOf(43));
		assertThat(events.get(0)).as("1st event").isEqualTo(e1);

		Event e2 = new Event(events.get(1).getId());
		e2.addValue(Event.TAG, "sleep");
		e2.setValue(Event.AUTHOR, TESTER);
		e2.setValue(Event.SOURCE, SleepsResult.SOURCE);
		e2.addValue(Event.TIMESTAMP, dateTime("2014-03-08T22:26:12-05:00"));
		e2.addValue(Event.TIMESTAMP, dateTime("2014-03-09T06:50:48-05:00"));
		e2.setValue(Event.DURATION, Duration.millis(30276000L));
		e2.setValue(Event.COUNT, 6);
		e2.setValue(Event.RATING, Rating.valueOf(80));
		e2.setValue(Event.PERCENTAGE, Percentage.valueOf(21));
		assertThat(events.get(1)).as("2nd event").isEqualTo(e2);

		Event e3 = new Event(events.get(2).getId());
		e3.addValue(Event.TAG, "sleep");
		e3.setValue(Event.AUTHOR, TESTER);
		e3.setValue(Event.SOURCE, SleepsResult.SOURCE);
		e3.addValue(Event.TIMESTAMP, dateTime("2014-03-07T22:12:58-05:00"));
		e3.addValue(Event.TIMESTAMP, dateTime("2014-03-08T04:09:22-05:00"));
		e3.setValue(Event.DURATION, Duration.millis(21384000L));
		e3.setValue(Event.COUNT, 12);
		e3.setValue(Event.RATING, Rating.valueOf(100));
		e3.setValue(Event.PERCENTAGE, Percentage.valueOf(48));
		assertThat(events.get(2)).as("3rd event").isEqualTo(e3);
	}
}
