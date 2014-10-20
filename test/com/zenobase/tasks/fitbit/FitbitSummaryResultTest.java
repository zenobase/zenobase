package com.zenobase.tasks.fitbit;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;

import org.joda.time.DateTime;
import org.junit.Test;

import com.zenobase.common.Units;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class FitbitSummaryResultTest extends ResultTestSupport {

	private static final String TAG = "steps";
	private static final DateTime TIMESTAMP = DateTime.now();

	@Test
	public void test() {
		FitbitSummaryResult result = new FitbitSummaryResult(readObject("FitbitStepsResultTest.json"), TAG, TESTER, TIMESTAMP, Units.MI, Units.FT, Units.KCAL);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(1);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TAG, "steps");
		expected.setValue(Event.COUNT, 9366);
		expected.setValue(Event.DISTANCE, DecimalMeasure.<Length>valueOf("4.47 mi"));
		expected.setValue(Event.HEIGHT, DecimalMeasure.<Length>valueOf("540 ft"));
		expected.setValue(Event.ENERGY, DecimalMeasure.<Energy>valueOf("1071 kcal"));
		expected.setValue(Event.TIMESTAMP, TIMESTAMP);
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, FitbitSummaryResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}

	@Test
	public void testEmpty() {
		FitbitSummaryResult result = new FitbitSummaryResult(Nodes.newObject(), TAG, TESTER, TIMESTAMP, Units.MI, Units.FT, Units.KCAL);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").isEmpty();
	}
}
