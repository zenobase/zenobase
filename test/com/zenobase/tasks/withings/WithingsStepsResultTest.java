package com.zenobase.tasks.withings;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class WithingsStepsResultTest extends ResultTestSupport {

	@Test
	public void test() {
		WithingsStepsResult result = new WithingsStepsResult(readObject("WithingsStepsResultTest.json"), TESTER, "walk", NonSI.MILE, NonSI.FOOT);
		assertThat(result.getStatus()).as("status").isEqualTo(0);
		List<Event> actual = result.getEvents();
		assertThat(actual).hasSize(2);
		Event expected = new Event(actual.get(0).getId());
		expected.setValue(Event.TIMESTAMP, DateTime.parse("2013-12-16T00:00:00.000-08:00"));
		expected.setValue(Event.DURATION, Duration.standardDays(1));
		expected.setValue(Event.TAG, "walk");
		expected.setValue(Event.COUNT, 13548);
		expected.setValue(Event.ENERGY, Measures.<Energy>valueOf("1231.8 cal"));
		expected.setValue(Event.DISTANCE, Measures.<Length>valueOf("7.87 mi"));
		expected.setValue(Event.HEIGHT, Measures.<Length>valueOf("1055.31 ft"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, WithingsStepsResult.SOURCE);
		assertThat(actual.get(0)).isEqualTo(expected);
	}
}
