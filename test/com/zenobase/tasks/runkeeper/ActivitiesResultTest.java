package com.zenobase.tasks.runkeeper;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class ActivitiesResultTest extends ResultTestSupport {

	@Test
	public void test() {
		Identity author = new Identity();
		ActivitiesResult result = new ActivitiesResult(readObject("ActivitiesResultTest.json"), author, Units.KM, Units.KCAL, DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.getNext()).isEqualTo("/fitnessActivities?page=1&pageSize=2");
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(2);
		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo("Hiking");
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2013-11-09T11:50:48-08:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.millis(16121187L));
		assertThat(events.get(0).getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf(new BigDecimal("6.16"), Units.KM));
		assertThat(events.get(0).getValue(Event.ENERGY)).isEqualTo(Measures.valueOf("1561 kcal"));
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(new Resource("RunKeeper", "/fitnessActivities/268390846"));
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(author);
	}
}
