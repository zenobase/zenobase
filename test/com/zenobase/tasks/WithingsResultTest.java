package com.zenobase.tasks;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Mass;

import org.joda.time.DateTime;
import org.junit.Test;

import com.zenobase.models.Event;

public class WithingsResultTest extends ResultTestSupport {

	@Test
	public void test() {
		WithingsResult result = new WithingsResult(TESTER, readObject("WithingsResultTest.json"));
		assertThat(result.getMarker()).as("marker").isEqualTo(1353615011L);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(2);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TAG, "body");
		expected.setValue(Event.WEIGHT, DecimalMeasure.<Mass>valueOf("71.550 kg"));
		expected.setValue(Event.TIMESTAMP, DateTime.parse("2012-11-22T17:49:17.000Z"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, WithingsResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
