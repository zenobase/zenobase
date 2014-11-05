package com.zenobase.tasks.strava;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class ActivitiesResultTest extends ResultTestSupport {

	@Test
	public void testMetric() {
		ActivitiesResult result = new ActivitiesResult(readArray("ActivitiesResultTest.json"), TESTER, true);
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);
		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo("Ride");
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(DateTime.parse("2013-08-23T17:04:12-07:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.standardSeconds(5427));
		assertThat(events.get(0).getValue(Event.LOCATION)).isEqualTo(new Location("37.793551", "-122.2686"));
		assertThat(events.get(0).getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("32.49 km"));
		assertThat(events.get(0).getValue(Event.HEIGHT)).isEqualTo(Measures.valueOf("566.00 m"));
		assertThat(events.get(0).getValue(Event.ENERGY)).isEqualTo(Measures.<Energy>valueOf("857.6 kJ"));
		assertThat(events.get(0).getValue(Event.VELOCITY)).isEqualTo(Measures.valueOf("12.24 kmh"));
		assertThat(events.get(0).getValue(Event.PACE)).isEqualTo(Measures.valueOf("294 s/km"));
		assertThat(events.get(0).getValue(Event.FREQUENCY)).isEqualTo(Measures.<Frequency>valueOf("139 bpm"));
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(new Resource("Strava", "http://www.strava.com/activities/8529483"));
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}

	@Test
	public void testImperial() {
		ActivitiesResult result = new ActivitiesResult(readArray("ActivitiesResultTest.json"), TESTER, false);
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);
		assertThat(events.get(0).getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("20.19 mi"));
		assertThat(events.get(0).getValue(Event.HEIGHT)).isEqualTo(Measures.valueOf("1856.96 ft"));
		assertThat(events.get(0).getValue(Event.ENERGY)).isEqualTo(Measures.<Energy>valueOf("857.6 kJ"));
		assertThat(events.get(0).getValue(Event.VELOCITY)).isEqualTo(Measures.valueOf("7.61 mph"));
		assertThat(events.get(0).getValue(Event.PACE)).isEqualTo(Measures.valueOf("473 s/mi"));
	}
}
