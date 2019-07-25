package com.zenobase.tasks.garmin;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class GarminEpochsResultTest extends ResultTestSupport {

	@Test
	public void test() {

		GarminEpochsResult result = new GarminEpochsResult(readObject("GarminEpochsApr24ResultTest.json"), TESTER, "Epoch");
		List<Event> events = result.getEvents();
		dump(events);
		assertThat(events).hasSize(24);

		assertThat(events.get(8).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2020-03-02T08:00:00-08:00"));
		assertThat(events.get(8).getValue(Event.DURATION)).isEqualTo(Duration.standardHours(1));
		assertThat(events.get(8).getValue(Event.COUNT)).isEqualTo(464);
		assertThat(events.get(8).getValue(Event.ENERGY)).isEqualTo(Measures.valueOf("34 kcal"));
		assertThat(events.get(8).getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("386 m"));
		assertThat(events.get(8).getValue(Event.SOURCE)).isEqualTo(GarminEpochsResult.SOURCE);
		assertThat(events.get(8).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}

	private static void dump(List<Event> events) {
		System.out.println(Joiner.on(",").join("timestamp", "count", "energy", "distance"));
		for (Event event : events) {
			System.out.println(Joiner.on(",").join(
				event.getValue(Event.TIMESTAMP),
				Objects.firstNonNull(event.getValue(Event.COUNT), 0),
				getValue(event.getValue(Event.ENERGY)),
				getValue(event.getValue(Event.DISTANCE)))
			);
		}
	}

	private static BigDecimal getValue(DecimalMeasure<?> measure) {
		return measure != null ? measure.getValue() : BigDecimal.ZERO;
	}
}
