package com.zenobase.tasks.withings;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class WithingsStepsResultTest extends ResultTestSupport {

	@Test
	@Ignore
	public void test() {
		WithingsStepsResult result = new WithingsStepsResult(readObject("WithingsStepsResultTest.json"), TESTER, "walk", NonSI.MILE, DateTimeZone.forID("Europe/Amsterdam"));
		assertThat(result.getStatus()).as("status").isEqualTo(0);
		// assertThat(result.getMarker()).as("marker").isEqualTo("1353615011");
		List<Event> events = result.getEvents();
		// assertThat(events).as("events").hasSize(2);
		assertThat(events).as("events").isNotEmpty();
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, DateTime.parse("2013-12-21T13:00:00.000+01:00"));
		expected.setValue(Event.TAG, "walk");
		expected.setValue(Event.COUNT, 1);
		expected.setValue(Event.DISTANCE, Measures.<Length>valueOf("1 mi"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, WithingsStepsResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
