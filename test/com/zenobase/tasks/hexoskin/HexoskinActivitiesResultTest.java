package com.zenobase.tasks.hexoskin;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class HexoskinActivitiesResultTest extends ResultTestSupport {

	@Test
	public void test() {

		HexoskinActivitiesResult result = new HexoskinActivitiesResult(readObject("HexoskinActivitiesResultTest.json"), TESTER, "Test", DateTimeZone.forID("America/Los_Angeles"), false);
		assertThat(result.next()).isNull();
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(7);

		assertThat(events.get(0).getValues(Event.TAG)).containsExactly("Test");
		assertThat(events.get(0).getValues(Event.TIMESTAMP)).containsExactly(dateTime("2013-07-31T15:03:42-07:00"), dateTime("2013-07-31T16:44:37-07:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.standardSeconds(6055));
		assertThat(events.get(0).getValue(Event.ENERGY)).isEqualTo(Measures.valueOf("830 kcal"));
		assertThat(events.get(0).getValue(Event.FREQUENCY)).isEqualTo(Measures.valueOf("149 bpm"));
		assertThat(events.get(0).getValue(Event.COUNT)).isEqualTo(Integer.valueOf(5679));
		assertThat(events.get(0).getValue(Event.PERCENTAGE)).isNull();
		assertThat(events.get(0).getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("0.62 mi"));
		assertThat(events.get(0).getValue(Event.NOTE)).isNull();
		assertThat(events.get(0).getValue(Event.RESOURCE)).isEqualTo(new Resource("Free program", "https://my.hexoskin.com/en/activities/26879"));
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(HexoskinActivitiesResult.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
