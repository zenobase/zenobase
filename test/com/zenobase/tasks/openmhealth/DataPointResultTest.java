package com.zenobase.tasks.openmhealth;

import static org.fest.assertions.Assertions.assertThat;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.VolumetricDensity;

import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.tasks.ResultTestSupport;

public class DataPointResultTest extends ResultTestSupport {

	@Test
	public void testEmpty() {
		Event actual = new DataPointResult(TESTER, Nodes.newObject()).getEvent();
		assertThat(actual).isNull();
	}

	@Test
	public void testMinimal() {
		Event actual = new DataPointResult(TESTER, readObject("DataPointResultTest-minimal.json")).getEvent();
		Event expected = new Event(actual.getId());
		expected.setValue(Event.TIMESTAMP, dateTime("2013-02-05T07:25:00Z"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, DataPointResult.SOURCE);
		expected.addValue(Event.TAG, "minimal");
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void testComplete() {
		Event actual = new DataPointResult(TESTER, readObject("DataPointResultTest.json")).getEvent();
		Event expected = new Event(actual.getId());
		expected.addValue(Event.TIMESTAMP, dateTime("2016-01-15T06:00:00-05:00"));
		expected.addValue(Event.TIMESTAMP, dateTime("2016-01-15T06:30:00-05:00"));
		expected.setValue(Event.DURATION, Duration.standardMinutes(30));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, DataPointResult.SOURCE);
		expected.addValue(Event.CONCENTRATION, DecimalMeasure.valueOf("120 mg/dL"));
		expected.addValue(Event.PRESSURE, DecimalMeasure.valueOf("160 mmHg"));
		expected.addValue(Event.PRESSURE, DecimalMeasure.valueOf("60 mmHg"));
		expected.addValue(Event.TEMPERATURE, DecimalMeasure.valueOf("97 F"));
		expected.addValue(Event.PERCENTAGE, Percentage.valueOf(16));
		expected.addValue(Event.HEIGHT, DecimalMeasure.valueOf("180 cm"));
		expected.addValue(Event.WEIGHT, DecimalMeasure.valueOf("50 kg"));
		expected.addValue(Event.ENERGY, DecimalMeasure.valueOf("160 kcal"));
		expected.addValue(Event.FREQUENCY, DecimalMeasure.valueOf("50 bpm"));
		expected.addValue(Event.DISTANCE, DecimalMeasure.valueOf("3.1 mi"));
		expected.addValue(Event.COUNT, 6000);
		expected.addValue(Event.TAG, "complete");
		expected.addValue(Event.TAG, "forehead");
		expected.addValue(Event.TAG, "walking");
		expected.addValue(Event.TAG, "at rest");
		expected.addValue(Event.NOTE, "just testing");
		assertThat(actual).isEqualTo(expected);
	}
}
