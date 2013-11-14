package com.zenobase.tasks.runkeeper;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.unit.SI;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.ResultTestSupport;

public class ActivitiesResultTest extends ResultTestSupport {

	@Test
	public void test() {
		Identity author = new Identity();
		ActivitiesResult result = new ActivitiesResult(readObject("ActivitiesResultTest.json"), author, SI.KILOMETER, DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.getNext()).isEqualTo("/fitnessActivities?page=1&pageSize=2");
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(2);
		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo("Hiking");
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(DateTime.parse("2013-11-09T11:50:48-08:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.millis(16121187L));
		assertThat(events.get(0).getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf(new BigDecimal("6.16"), SI.KILOMETER));
		assertThat(events.get(0).getValue(Event.ENERGY)).isEqualTo(Measures.<Energy>valueOf(new BigDecimal("1561"), "cal"));
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(ActivitiesResult.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(author);
	}
}
