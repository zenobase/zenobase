package com.zenobase.tasks.oura;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;

public class OuraStepsResultTest extends ResultTestSupport {

	@Test
	public void test() {

		OuraStepsResult result = new OuraStepsResult(readObject("OuraStepsResultTest.json"), TESTER, "Steps");
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(3);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo("Steps");
		assertThat(events.get(0).getValues(Event.TIMESTAMP)).containsSequence(dateTime("2018-12-01T04:00:00+02:00"), dateTime("2018-12-02T03:59:59+02:00"));
		assertThat(events.get(0).getValue(Event.COUNT)).isEqualTo(15604);
		assertThat(events.get(0).getValue(Event.ENERGY)).isEqualTo(Measures.valueOf("3039 kcal"));
		assertThat(events.get(0).getValue(Event.RATING)).isEqualTo(Rating.valueOf(93));
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(OuraStepsResult.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
