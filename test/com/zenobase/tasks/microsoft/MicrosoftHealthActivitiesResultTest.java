package com.zenobase.tasks.microsoft;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

public class MicrosoftHealthActivitiesResultTest extends ResultTestSupport {

	@Test
	public void test() {
		MicrosoftHealthActivitiesResult result = new MicrosoftHealthActivitiesResult(readObject("MicrosoftHealthActivitiesResultTest.json"), TESTER, DateTimeZone.forID("Europe/Berlin"), true);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(6);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, dateTime("2015-07-11T17:42:57+02:00"));
		expected.addValue(Event.TAG, "Run");
		expected.setValue(Event.DURATION, Duration.standardSeconds(3303));
		expected.setValue(Event.DISTANCE, Measures.<Length>valueOf("2.83 km"));
		expected.setValue(Event.HEIGHT, Measures.<Length>valueOf("48 m"));
		expected.setValue(Event.FREQUENCY, Measures.<Frequency>valueOf("81 bpm"));
		expected.setValue(Event.ENERGY, Measures.<Energy>valueOf("175 kcal"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, MicrosoftHealthActivitiesResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}

	@Test
	public void testImperialUnits() {
		MicrosoftHealthActivitiesResult result = new MicrosoftHealthActivitiesResult(readObject("MicrosoftHealthActivitiesResultTest.json"), TESTER, DateTimeZone.forID("Europe/Berlin"), false);
		List<Event> events = result.getEvents();
		assertThat(events.get(0).getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("1.76 mi"));
		assertThat(events.get(0).getValue(Event.HEIGHT)).isEqualTo(Measures.valueOf("157 ft"));
	}
}
