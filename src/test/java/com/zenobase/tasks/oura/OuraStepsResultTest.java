package com.zenobase.tasks.oura;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;

public class OuraStepsResultTest extends ResultTestSupport {

	@Test
	public void test() {

		OuraStepsResult result = new OuraStepsResult(
				readObject("OuraStepsResultTest.json"), TESTER, "Steps", DateTimeZone.forID("Europe/Berlin"));
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(3);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo("Steps");
		assertThat(events.get(0).getValues(Event.TIMESTAMP))
				.containsSequence(dateTime("2018-12-02T00:00:00+01:00"), dateTime("2018-12-03T00:00:00+01:00"));
		assertThat(events.get(0).getValue(Event.COUNT)).isEqualTo(15604);
		assertThat(events.get(0).getValue(Event.ENERGY)).isEqualTo(Measures.valueOf("3039 kcal"));
		assertThat(events.get(0).getValue(Event.RATING)).isEqualTo(Rating.valueOf(93));
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(OuraStepsResult.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
