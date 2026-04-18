package com.zenobase.tasks.oura;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.Duration;
import org.junit.jupiter.api.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.tasks.ResultTestSupport;

public class OuraSleepResultTest extends ResultTestSupport {

	@Test
	public void test() {
		OuraSleepResult result = new OuraSleepResult(readObject("OuraSleepResultTest.json"), TESTER, "Sleep");
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(3);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo("Sleep");
		assertThat(events.get(0).getValues(Event.TIMESTAMP)).containsSequence(
			dateTime("2018-12-02T21:46:27+02:00"),
			dateTime("2018-12-03T07:36:27+02:00")
		);
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.standardSeconds(35400));
		assertThat(events.get(0).getValue(Event.PERCENTAGE)).isEqualTo(Percentage.valueOf(88));
		assertThat(events.get(0).getValue(Event.FREQUENCY)).isEqualTo(Measures.valueOf("57 bpm"));
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(OuraStepsResult.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
