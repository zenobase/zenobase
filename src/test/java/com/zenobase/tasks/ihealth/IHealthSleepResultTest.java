package com.zenobase.tasks.ihealth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.tasks.ResultTestSupport;

public class IHealthSleepResultTest extends ResultTestSupport {

	@Test
	public void test() {

		String tag = "Sleep";
		IHealthSleepResult result = new IHealthSleepResult(readObject("IHealthSleepResultTest.json"), TESTER, tag, DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.hasNext()).isFalse();
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo(tag);
		assertThat(events.get(0).getValues(Event.TIMESTAMP)).containsExactly(dateTime("2014-12-10T23:00:00-08:00"), dateTime("2014-12-11T07:00:00-08:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.standardMinutes(450));
		assertThat(events.get(0).getValue(Event.PERCENTAGE)).isEqualTo(Percentage.valueOf(80));
		assertThat(events.get(0).getValue(Event.LOCATION)).isNull();
		assertThat(events.get(0).getValue(Event.NOTE)).isNull();
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(IHealthResultSupport.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
