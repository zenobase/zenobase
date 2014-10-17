package com.zenobase.tasks.fitbit;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class FitbitFoodResultTest extends ResultTestSupport {

	private static final String TAG = "eat";
	private static final DateTimeZone TIMEZONE = DateTimeZone.forID("America/Los_Angeles");

	@Test
	public void test() {
		FitbitFoodResult result = new FitbitFoodResult(readObject("FitbitFoodResultTest.json"), TAG, TESTER, TIMEZONE);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(3);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TAG, TAG);
		expected.setValue(Event.TIMESTAMP, DateTime.parse("2014-01-01T00:00:00-08:00"));
		expected.setValue(Event.DURATION, Duration.standardDays(1));
		expected.setValue(Event.ENERGY, DecimalMeasure.<Energy>valueOf("2500 kcal"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, FitbitFoodResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
