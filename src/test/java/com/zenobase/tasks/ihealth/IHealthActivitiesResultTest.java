package com.zenobase.tasks.ihealth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class IHealthActivitiesResultTest extends ResultTestSupport {

	@Test
	public void test() {

		String tag = "Activity";
		IHealthActivitiesResult result = new IHealthActivitiesResult(readObject("IHealthActivitiesResultTest.json"), TESTER, tag, DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.hasNext()).isFalse();
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);

		assertThat(events.get(0).getValues(Event.TAG)).containsExactly(tag, "Run");
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2014-12-11T10:00:00-08:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.standardHours(1));
		assertThat(events.get(0).getValue(Event.ENERGY)).isEqualTo(Measures.valueOf("1000 kcal"));
		assertThat(events.get(0).getValue(Event.LOCATION)).isNull();
		assertThat(events.get(0).getValue(Event.NOTE)).isNull();
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(IHealthResultSupport.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
