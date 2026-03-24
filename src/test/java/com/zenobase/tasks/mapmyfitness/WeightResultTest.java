package com.zenobase.tasks.mapmyfitness;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Mass;

import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.tasks.ResultTestSupport;

public class WeightResultTest extends ResultTestSupport {

	@Test
	public void test() {

		WeightResult result = new WeightResult(readObject("WeightResultTest.json"), TESTER, "mass", true);
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(2);

		Event actual = events.get(1);
		Event expected = new Event(actual.getId());
		expected.addValue(Event.TAG, "mass");
		expected.setValue(Event.TIMESTAMP, dateTime("2015-01-06T08:44:07-08:00"));
		expected.setValue(Event.WEIGHT, DecimalMeasure.valueOf("157.9 lb"));
		expected.setValue(Event.PERCENTAGE, Percentage.valueOf(new BigDecimal("11.299")));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, WeightResult.SOURCE);
		assertThat(actual).isEqualTo(expected);
	}
}
