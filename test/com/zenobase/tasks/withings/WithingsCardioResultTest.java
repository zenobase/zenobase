package com.zenobase.tasks.withings;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.DecimalMeasure;

import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.tasks.ResultTestSupport;

public class WithingsCardioResultTest extends ResultTestSupport {

	@Test
	public void test() {
		WithingsCardioResult result = new WithingsCardioResult(readObject("WithingsCardioResultTest.json"), TESTER, "heart rate", DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.getStatus()).as("status").isEqualTo(0);
		assertThat(result.getMarker()).as("marker").isEqualTo("1387899568");
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(3);
		checkFirst(events.get(0));
		checkLast(events.get(2));
	}

	private void checkFirst(Event event) {
		Event expected = new Event(event.getId());
		expected.setValue(Event.TAG, "heart rate");
		expected.setValue(Event.FREQUENCY, DecimalMeasure.valueOf("59 bpm"));
		expected.setValue(Event.PERCENTAGE, Percentage.valueOf(97));
		expected.setValue(Event.TIMESTAMP, dateTime("2013-12-22T22:59:41-08:00"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, WithingsWeightResult.SOURCE);
		assertThat(event).as("first event").isEqualTo(expected);
	}

	private void checkLast(Event event) {
		Event expected = new Event(event.getId());
		expected.setValue(Event.TAG, "heart rate");
		expected.setValue(Event.FREQUENCY, DecimalMeasure.valueOf("80 bpm"));
		expected.addValue(Event.PRESSURE, DecimalMeasure.valueOf("70 mmHg"));
		expected.addValue(Event.PRESSURE, DecimalMeasure.valueOf("110 mmHg"));
		expected.setValue(Event.TIMESTAMP, dateTime("2013-12-17T07:24:27-08:00"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, WithingsWeightResult.SOURCE);
		assertThat(event).as("last event").isEqualTo(expected);
	}
}
