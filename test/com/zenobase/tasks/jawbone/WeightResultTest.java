package com.zenobase.tasks.jawbone;

import static org.fest.assertions.Assertions.assertThat;

import javax.measure.quantity.Mass;

import org.junit.Test;
import com.google.common.collect.Iterables;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.tasks.ResultTestSupport;

public class WeightResultTest extends ResultTestSupport {

	@Test
	public void testKg() {
		WeightResult result = new WeightResult(readObject("WeightResultTest.json"), TESTER, "Body", true);
		Event found = Iterables.getOnlyElement(result.getEvents());
		Event expected = new Event(found.getId());
		expected.setValue(Event.TIMESTAMP, dateTime("2014-11-05T17:00:00Z"));
		expected.addValue(Event.TAG, "Body");
		expected.setValue(Event.WEIGHT, Measures.<Mass>valueOf("70.76 kg"));
		expected.setValue(Event.PERCENTAGE, Percentage.valueOf(12));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, JawboneResult.SOURCE);
		assertThat(found).isEqualTo(expected);
	}

	@Test
	public void testLb() {
		WeightResult result = new WeightResult(readObject("WeightResultTest.json"), TESTER, "Body", false);
		Event found = Iterables.getOnlyElement(result.getEvents());
		Event expected = new Event(found.getId());
		expected.setValue(Event.TIMESTAMP, dateTime("2014-11-05T17:00:00Z"));
		expected.addValue(Event.TAG, "Body");
		expected.setValue(Event.WEIGHT, Measures.<Mass>valueOf("156.00 lb"));
		expected.setValue(Event.PERCENTAGE, Percentage.valueOf(12));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, JawboneResult.SOURCE);
		assertThat(found).isEqualTo(expected);
	}
}
