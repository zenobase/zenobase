package com.zenobase.tasks.beddit;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Frequency;

import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.tasks.ResultTestSupport;

public class BedditResultTest extends ResultTestSupport {

	@Test
	public void test() {

		BedditResult result = new BedditResult("sleep", TESTER, dateTime("2015-02-03T23:57:52-08:00"), readArray("BedditResultTest.json"));
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);

		Event expected = new Event(events.get(0).getId());
		expected.addValue(Event.TAG, "sleep");
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, BedditResult.SOURCE);
		expected.addValue(Event.TIMESTAMP, dateTime("2015-02-03T23:57:52-08:00"));
		expected.addValue(Event.TIMESTAMP, dateTime("2015-02-04T08:09:24-08:00"));
		expected.setValue(Event.DURATION, Duration.standardSeconds(29492));
		expected.setValue(Event.PERCENTAGE, Percentage.valueOf(97));
		expected.setValue(Event.FREQUENCY, Measures.valueOf("60 bpm"));
		assertThat(events.get(0)).isEqualTo(expected);
	}
}
