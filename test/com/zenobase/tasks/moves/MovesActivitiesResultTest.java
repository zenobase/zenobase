package com.zenobase.tasks.moves;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class MovesActivitiesResultTest extends ResultTestSupport {

	@Test
	public void test() {
		DateTime begin = dateTime("2012-12-12T07:14:30+0200");
		MovesActivitiesResult result = new MovesActivitiesResult(readArray("MovesActivitiesResultTest.json"), TESTER, begin, Units.KM, Units.KCAL);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(6);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, begin);
		expected.addValue(Event.TAG, "walking");
		expected.setValue(Event.DURATION, Duration.standardSeconds(782));
		expected.setValue(Event.DISTANCE, Measures.<Length>valueOf("1.25 km"));
		expected.setValue(Event.COUNT, 1353);
		expected.setValue(Event.ENERGY, Measures.<Energy>valueOf("99 kcal"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, MovesPlacesResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
