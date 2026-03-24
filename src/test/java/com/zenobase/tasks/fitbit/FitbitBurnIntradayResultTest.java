package com.zenobase.tasks.fitbit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class FitbitBurnIntradayResultTest extends ResultTestSupport {

	private static final String TAG = "hour";
	private static final LocalDate DATE = LocalDate.parse("2016-04-25");
	private static final DateTimeZone TIMEZONE = DateTimeZone.forID("America/Los_Angeles");

	@Test
	public void test() {
		FitbitBurnIntradayResult result = new FitbitBurnIntradayResult(readObject("FitbitBurnIntradayResultTest.json"), TAG, TESTER, DATE, TIMEZONE);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(24);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TAG, TAG);
		expected.setValue(Event.TIMESTAMP, dateTime("2016-04-25T00:00:00-07:00"));
		expected.setValue(Event.DURATION, Duration.standardHours(1));
		expected.setValue(Event.ENERGY, Measures.valueOf("85 kcal"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, FitbitBurnIntradayResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
		assertThat(events.get(8).getValue(Event.ENERGY)).as("08:00").isEqualTo(Measures.valueOf("102 kcal"));
	}

	@Test
	public void testEmpty() {
		FitbitBurnIntradayResult result = new FitbitBurnIntradayResult(Nodes.newObject(), TAG, TESTER, DATE, TIMEZONE);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").isEmpty();
	}
}
