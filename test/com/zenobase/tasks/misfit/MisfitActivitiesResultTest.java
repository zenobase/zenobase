package com.zenobase.tasks.misfit;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class MisfitActivitiesResultTest extends ResultTestSupport {

	@Test
	public void test() {

		MisfitActivitiesResult result = new MisfitActivitiesResult(readObject("MisfitActivitiesResultTest.json"), TESTER, dateTime("2014-12-08T20:00:00-08:00"));
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo("Walking");
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2014-12-08T20:30:00-08:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.standardMinutes(30));
		assertThat(events.get(0).getValue(Event.COUNT)).isEqualTo(2500);
		assertThat(events.get(0).getValue(Event.ENERGY)).isEqualTo(Measures.valueOf("300 kcal"));
		assertThat(events.get(0).getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("1.5 mi"));
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(MisfitActivitiesResult.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
