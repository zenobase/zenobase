package com.zenobase.tasks.misfit;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.tasks.ResultTestSupport;

public class MisfitSleepResultTest extends ResultTestSupport {

	@Test
	public void test() {

		MisfitSleepResult result = new MisfitSleepResult(readObject("MisfitSleepResultTest.json"), TESTER, "Sleep");
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo("Sleep");
		assertThat(events.get(0).getValues(Event.TIMESTAMP)).containsExactly(dateTime("2014-12-08T21:46:29+02:00"), dateTime("2014-12-09T07:33:29+02:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.standardMinutes(587));
		assertThat(events.get(0).getValue(Event.PERCENTAGE)).isEqualTo(Percentage.valueOf(75));
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(MisfitActivitiesResult.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
