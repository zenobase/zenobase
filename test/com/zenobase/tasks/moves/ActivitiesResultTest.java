package com.zenobase.tasks.moves;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class ActivitiesResultTest extends ResultTestSupport {

	@Test
	public void test() {
		DateTime begin = DateTime.parse("20121212T071430+0200", ISODateTimeFormat.basicDateTimeNoMillis().withOffsetParsed());
		ActivitiesResult result = new ActivitiesResult(TESTER, begin, SI.KILOMETER, readArray("ActivitiesResultTest.json"));
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(6);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, begin);
		expected.addValue(Event.TAG, "walking");
		expected.setValue(Event.DURATION, Duration.standardSeconds(782));
		expected.setValue(Event.DISTANCE, Measures.<Length>valueOf("1.25 km"));
		expected.setValue(Event.COUNT, 1353);
		expected.setValue(Event.ENERGY, Measures.<Energy>valueOf("99 cal"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, PlacesResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
