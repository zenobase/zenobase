package com.zenobase.tasks;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;

import org.joda.time.DateTime;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class FitbitActivitiesResultTest extends ResultTestSupport {

	private static final String TAG = "steps";
	private static final DateTime TIMESTAMP = DateTime.now();

	@Test
	public void test() {
		FitbitActivitiesResult result = new FitbitActivitiesResult(readObject("FitbitActivitiesResultTest.json"), TAG, TESTER, TIMESTAMP, NonSI.MILE, NonSI.FOOT);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(1);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TAG, "steps");
		expected.setValue(Event.COUNT, 9366);
		expected.setValue(Event.DISTANCE, DecimalMeasure.<Length>valueOf("4.47 mi"));
		expected.setValue(Event.HEIGHT, DecimalMeasure.<Length>valueOf("540 ft"));
		expected.setValue(Event.ENERGY, DecimalMeasure.<Energy>valueOf("1071 cal"));
		expected.setValue(Event.TIMESTAMP, TIMESTAMP);
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, FitbitActivitiesResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}

	@Test
	public void testEmpty() {
		FitbitActivitiesResult result = new FitbitActivitiesResult(Nodes.newObject(), TAG, TESTER, TIMESTAMP, NonSI.MILE, NonSI.FOOT);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").isEmpty();
	}
}
