package com.zenobase.tasks.fitbit;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Frequency;

import com.zenobase.common.Measures;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.junit.Test;

public class FitbitCardioIntradayResultTest extends ResultTestSupport {

	private static final String TAG = "hr";
	private static final LocalDate DATE = LocalDate.parse("2015-07-28");
	private static final DateTimeZone TIMEZONE = DateTimeZone.forID("America/Los_Angeles");

	@Test
	public void test() {
		FitbitCardioIntradayResult result = new FitbitCardioIntradayResult(readObject("FitbitCardioIntradayResultTest.json"), TAG, TESTER, DATE, TIMEZONE);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(24);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TAG, TAG);
		expected.setValue(Event.TIMESTAMP, dateTime("2015-07-28T00:00:00-07:00"));
		expected.setValue(Event.DURATION, Duration.standardHours(1));
		expected.setValue(Event.FREQUENCY, Measures.<Frequency>valueOf("56 bpm"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, FitbitIntradayStepsResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
		assertThat(events.get(8).getValue(Event.FREQUENCY)).as("08:00").isEqualTo(Measures.valueOf("78 bpm"));
	}

	@Test
	public void testEmpty() {
		FitbitCardioIntradayResult result = new FitbitCardioIntradayResult(Nodes.newObject(), TAG, TESTER, DATE, TIMEZONE);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").isEmpty();
	}
}
