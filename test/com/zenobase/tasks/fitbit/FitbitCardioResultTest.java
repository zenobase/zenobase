package com.zenobase.tasks.fitbit;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Frequency;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

public class FitbitCardioResultTest extends ResultTestSupport {

	private static final String TAG = "hr";
	private static final DateTimeZone TIMEZONE = DateTimeZone.forID("America/Los_Angeles");

	@Test
	public void test() {
		FitbitCardioResult result = new FitbitCardioResult(readObject("FitbitCardioResultTest.json"), TAG, TESTER, TIMEZONE);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(8);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TAG, TAG);
		expected.setValue(Event.TIMESTAMP, dateTime("2015-07-25T00:00:00-07:00"));
		expected.setValue(Event.DURATION, Duration.standardDays(1));
		expected.setValue(Event.FREQUENCY, DecimalMeasure.<Frequency>valueOf("61 bpm"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, FitbitCardioResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}

	@Test
	public void testEmpty() {
		FitbitCardioResult result = new FitbitCardioResult(Nodes.newObject(), TAG, TESTER, TIMEZONE);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").isEmpty();
	}
}
