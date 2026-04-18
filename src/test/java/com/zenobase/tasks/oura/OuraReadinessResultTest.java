package com.zenobase.tasks.oura;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;
import java.util.List;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class OuraReadinessResultTest extends ResultTestSupport {

	@Test
	public void test() {
		OuraReadinessResult result = new OuraReadinessResult(
			readObject("OuraReadinessResultTest.json"),
			TESTER,
			"Readiness",
			DateTimeZone.forID("Europe/Berlin")
		);
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(3);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo("Readiness");
		assertThat(events.get(0).getValues(Event.TIMESTAMP)).containsSequence(
			dateTime("2018-12-01T00:00:00+01:00"),
			dateTime("2018-12-02T00:00:00+01:00")
		);
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(OuraStepsResult.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
