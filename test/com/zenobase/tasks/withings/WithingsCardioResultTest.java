package com.zenobase.tasks.withings;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Frequency;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class WithingsCardioResultTest extends ResultTestSupport {

	@Test
	public void test() {
		WithingsCardioResult result = new WithingsCardioResult(readObject("WithingsCardioResultTest.json"), TESTER, "heart rate", DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.getStatus()).as("status").isEqualTo(0);
		assertThat(result.getMarker()).as("marker").isEqualTo("1387899568");
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(2);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TAG, "heart rate");
		expected.setValue(Event.FREQUENCY, DecimalMeasure.<Frequency>valueOf("59 bpm"));
		expected.setValue(Event.TIMESTAMP, DateTime.parse("2013-12-22T22:59:41.000-08:00"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, WithingsWeightResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
		assertThat(events.get(1).getValue(Event.TAG)).as("second event").isEqualTo("heart rate");
	}
}
