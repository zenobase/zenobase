package com.zenobase.tasks.fitbit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class FitbitIntradayStepsResultTest extends ResultTestSupport {

	private static final String TAG = "walk";
	private static final LocalDate DATE = LocalDate.parse("2014-10-20");
	private static final DateTimeZone TIMEZONE = DateTimeZone.forID("America/Los_Angeles");

	@Test
	public void test() {
		FitbitIntradayStepsResult result = new FitbitIntradayStepsResult(
				readObject("FitbitIntradayStepsResultTest.json"), TAG, TESTER, DATE, TIMEZONE);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(24);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TAG, TAG);
		expected.setValue(Event.TIMESTAMP, dateTime("2014-10-20T00:00:00-07:00"));
		expected.setValue(Event.DURATION, Duration.standardHours(1));
		expected.setValue(Event.COUNT, 0);
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, FitbitIntradayStepsResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
		assertThat(events.get(8).getValue(Event.COUNT)).as("08:00").isEqualTo(186);
	}

	@Test
	public void testEmpty() {
		FitbitIntradayStepsResult result =
				new FitbitIntradayStepsResult(Nodes.newObject(), TAG, TESTER, DATE, TIMEZONE);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").isEmpty();
	}
}
