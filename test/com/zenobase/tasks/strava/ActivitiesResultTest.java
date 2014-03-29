package com.zenobase.tasks.strava;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.unit.SI;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class ActivitiesResultTest extends ResultTestSupport {

	@Test
	public void test() {
		Identity author = new Identity();
		ActivitiesResult result = new ActivitiesResult(readArray("ActivitiesResultTest.json"), author, true);
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);
		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo("Ride");
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(DateTime.parse("2013-08-23T17:04:12-07:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.standardSeconds(5427));
		assertThat(events.get(0).getValue(Event.LOCATION)).isEqualTo(new Location("37.793551", "-122.2686"));
		assertThat(events.get(0).getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf(new BigDecimal("32.49"), SI.KILOMETER));
		assertThat(events.get(0).getValue(Event.HEIGHT)).isEqualTo(Measures.valueOf(new BigDecimal("566.00"), SI.METER));
		assertThat(events.get(0).getValue(Event.ENERGY)).isEqualTo(Measures.<Energy>valueOf(new BigDecimal("857.6"), "kJ"));
		assertThat(events.get(0).getValue(Event.FREQUENCY)).isEqualTo(Measures.<Frequency>valueOf(new BigDecimal("138.8"), "bpm"));
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(new Resource("Strava", "http://www.strava.com/activities/8529483"));
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(author);
	}
}
