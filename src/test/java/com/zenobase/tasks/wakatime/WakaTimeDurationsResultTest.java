package com.zenobase.tasks.wakatime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class WakaTimeDurationsResultTest extends ResultTestSupport {

	@Test
	public void test() {

		WakaTimeDurationsResult result =
				new WakaTimeDurationsResult(readObject("WakaTimeDurationsResultTest.json"), TESTER, "project");
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(12);

		assertThat(events.get(0).getValues(Event.TAG)).containsExactly("project", "zenobase");
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2015-05-05T00:15:53-07:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.millis(6359));
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(WakaTimeDurationsResult.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
