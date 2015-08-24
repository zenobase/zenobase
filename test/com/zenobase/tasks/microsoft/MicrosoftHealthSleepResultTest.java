package com.zenobase.tasks.microsoft;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.tasks.ResultTestSupport;

public class MicrosoftHealthSleepResultTest extends ResultTestSupport {

	@Test
	public void test() {
		MicrosoftHealthSleepResult result = new MicrosoftHealthSleepResult(readObject("MicrosoftHealthSleepResultTest.json"), TESTER, DateTimeZone.forID("Europe/Berlin"), "zzz");
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(2);
		Event expected = new Event(events.get(0).getId());
		expected.addValue(Event.TIMESTAMP, dateTime("2015-02-26T23:00:00+01:00"));
		expected.addValue(Event.TIMESTAMP, dateTime("2015-02-27T06:00:00+01:00"));
		expected.addValue(Event.TAG, "zzz");
		expected.setValue(Event.DURATION, Duration.standardHours(7));
		expected.setValue(Event.PERCENTAGE, Percentage.valueOf(95));
		expected.setValue(Event.FREQUENCY, Measures.<Frequency>valueOf("50 bpm"));
		expected.setValue(Event.ENERGY, Measures.<Energy>valueOf("465 kcal"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, MicrosoftHealthSleepResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
