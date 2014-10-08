package com.zenobase.tasks.fitbit;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Mass;
import javax.measure.unit.SI;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.tasks.ResultTestSupport;

public class FitbitWeightResultTest extends ResultTestSupport {

	private static final String TAG = "body";
	private static final LocalDate DATE = LocalDate.parse("2014-10-08");
	private static final DateTimeZone TIMEZONE = DateTimeZone.forID("America/Los_Angeles");

	@Test
	public void test() {
		FitbitWeightResult result = new FitbitWeightResult(readObject("FitbitWeightResultTest.json"), TAG, TESTER, DATE, TIMEZONE, SI.KILOGRAM);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(1);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TAG, TAG);
		expected.setValue(Event.TIMESTAMP, DateTime.parse("2014-10-08T00:00:00-07:00"));
		expected.setValue(Event.WEIGHT, DecimalMeasure.<Mass>valueOf("72.6 kg"));
		expected.setValue(Event.PERCENTAGE, Percentage.valueOf(13));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, FitbitWeightResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}

	@Test
	public void testEmpty() {
		FitbitWeightResult result = new FitbitWeightResult(Nodes.newObject(), TAG, TESTER, DATE, TIMEZONE, SI.KILOGRAM);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").isEmpty();
	}
}
