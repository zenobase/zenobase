package com.zenobase.tasks.withings;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;

import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.tasks.ResultTestSupport;

public class WithingsHealthWeightResultTest extends ResultTestSupport {

	@Test
	public void test() {
		WithingsHealthWeightResult result = new WithingsHealthWeightResult(readObject("WithingsHealthWeightResultTest.json"), TESTER, "body", Units.LB, DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.getStatus()).as("status").isEqualTo(0);
		assertThat(result.getMarker()).as("marker").isEqualTo("1353615011");
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(2);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TAG, "body");
		expected.setValue(Event.WEIGHT, DecimalMeasure.valueOf("157.74 lb"));
		expected.setValue(Event.PERCENTAGE, Percentage.valueOf(new BigDecimal("13.459")));
		expected.setValue(Event.TIMESTAMP, dateTime("2012-11-22T09:49:17-08:00"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, WithingsHealthWeightResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
