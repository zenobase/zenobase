package com.zenobase.tasks.garmin;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.tasks.ResultTestSupport;

public class GarminActivitiesResultTest extends ResultTestSupport {

	@Test
	public void test() {

		GarminActivitiesResult result = new GarminActivitiesResult(readObject("GarminActivitiesResultTest.json"), TESTER);
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(2);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo("breathwork");
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2020-04-12T19:25:42-07:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.standardSeconds(934));
		assertThat(events.get(0).getValue(Event.FREQUENCY)).isEqualTo(Measures.valueOf("69 bpm"));
		assertThat(events.get(0).getValue(Event.COUNT)).isNull();
		assertThat(events.get(0).getValue(Event.DISTANCE)).isNull();
		assertThat(events.get(0).getValue(Event.HEIGHT)).isNull();
		assertThat(events.get(0).getValue(Event.LOCATION)).isNull();
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(GarminActivitiesResult.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);

		assertThat(events.get(1).getValue(Event.TAG)).isEqualTo("hiking");
		assertThat(events.get(1).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2020-04-24T10:37:47-07:00"));
		assertThat(events.get(1).getValue(Event.DURATION)).isEqualTo(Duration.standardSeconds(18831));
		assertThat(events.get(1).getValue(Event.FREQUENCY)).isEqualTo(Measures.valueOf("133 bpm"));
		assertThat(events.get(1).getValue(Event.COUNT)).isEqualTo(16978);
		assertThat(events.get(1).getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("12378 m"));
		assertThat(events.get(1).getValue(Event.HEIGHT)).isEqualTo(Measures.valueOf("1062 m"));
		assertThat(events.get(1).getValue(Event.LOCATION)).isEqualTo(new Location("47.2674071", "-121.1742735"));
		assertThat(events.get(1).getValue(Event.SOURCE)).isEqualTo(GarminActivitiesResult.SOURCE);
		assertThat(events.get(1).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
