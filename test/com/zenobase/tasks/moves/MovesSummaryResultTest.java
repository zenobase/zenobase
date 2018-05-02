package com.zenobase.tasks.moves;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;

import org.joda.time.DateTime;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class MovesSummaryResultTest extends ResultTestSupport {

	@Test
	public void test() {
		DateTime begin = dateTime("2013-03-15T00:00:00+0200");
		MovesSummaryResult result = new MovesSummaryResult(readArray("MovesSummaryResultTest.json"), TESTER, begin.getZone(), "steps", Units.KM);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(2);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, begin);
		expected.addValue(Event.TAG, "steps");
		expected.setValue(Event.DISTANCE, Measures.<Length>valueOf("2.28 km"));
		expected.setValue(Event.COUNT, 3124);
		expected.setValue(Event.ENERGY, Measures.<Energy>valueOf("159 kcal"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, MovesSummaryResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
