package com.zenobase.tasks.fitbit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.measure.DecimalMeasure;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;

import com.zenobase.common.Units;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class FitbitStepsResultTest extends ResultTestSupport {

	private static final String TAG = "walk";
	private static final LocalDate DATE = LocalDate.parse("2013-11-03");
	private static final DateTimeZone TIMEZONE = DateTimeZone.forID("America/Los_Angeles");

	@Test
	public void test() {
		FitbitStepsResult result = new FitbitStepsResult(
				readObject("FitbitStepsResultTest.json"),
				TAG,
				TESTER,
				DATE,
				TIMEZONE,
				Units.MI,
				Units.FT,
				Units.KCAL,
				true);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(1);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TAG, TAG);
		expected.setValue(Event.TIMESTAMP, dateTime("2013-11-03T00:00:00-07:00"));
		expected.setValue(Event.DURATION, Duration.standardHours(25));
		expected.setValue(Event.COUNT, 9366);
		expected.setValue(Event.DISTANCE, DecimalMeasure.valueOf("4.47 mi"));
		expected.setValue(Event.HEIGHT, DecimalMeasure.valueOf("540 ft"));
		expected.setValue(Event.ENERGY, DecimalMeasure.valueOf("2535 kcal"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, FitbitStepsResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}

	@Test
	public void testEmpty() {
		FitbitStepsResult result = new FitbitStepsResult(
				Nodes.newObject(), TAG, TESTER, DATE, TIMEZONE, Units.MI, Units.FT, Units.KCAL, false);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").isEmpty();
	}
}
