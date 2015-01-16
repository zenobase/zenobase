package com.zenobase.tasks.hexoskin;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class HexoskinSleepResultTest extends ResultTestSupport {

	@Test
	public void test() {

		HexoskinSleepResult result = new HexoskinSleepResult(readObject("HexoskinSleepResultTest.json"), TESTER, "Test", DateTimeZone.forID("America/Los_Angeles"), false);
		assertThat(result.next()).isNull();
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);

		assertThat(events.get(0).getValues(Event.TAG)).containsExactly("Test");
		assertThat(events.get(0).getValues(Event.TIMESTAMP)).containsExactly(dateTime("2014-03-26T03:56:00-07:00"), dateTime("2014-03-27T00:00:00-07:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.standardMinutes(1204));
		assertThat(events.get(0).getValue(Event.ENERGY)).isEqualTo(Measures.valueOf("301 kcal"));
		assertThat(events.get(0).getValue(Event.FREQUENCY)).isEqualTo(Measures.valueOf("53 bpm"));
		assertThat(events.get(0).getValue(Event.COUNT)).isNull();
		assertThat(events.get(0).getValue(Event.PERCENTAGE)).isEqualTo(Percentage.valueOf(80));
		assertThat(events.get(0).getValue(Event.DISTANCE)).isNull();
		assertThat(events.get(0).getValue(Event.NOTE)).isEqualTo("test");
		assertThat(events.get(0).getValue(Event.RESOURCE)).isEqualTo(new Resource("Sleep", "https://my.hexoskin.com/en/activities/37441"));
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(HexoskinResultSupport.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
