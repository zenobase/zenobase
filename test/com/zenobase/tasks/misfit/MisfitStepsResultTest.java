package com.zenobase.tasks.misfit;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class MisfitStepsResultTest extends ResultTestSupport {

	@Test
	public void test() {

		MisfitStepsResult result = new MisfitStepsResult(readObject("MisfitStepsResultTest.json"), TESTER, "Steps", DateTimeZone.forID("America/Los_Angeles"));
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo("Steps");
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2014-11-02T00:00:00-07:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.standardHours(25));
		assertThat(events.get(0).getValue(Event.COUNT)).isEqualTo(106);
		assertThat(events.get(0).getValue(Event.ENERGY)).isEqualTo(Measures.valueOf("2044 kcal"));
		assertThat(events.get(0).getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("0.0401 mi"));
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(MisfitActivitiesResult.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
