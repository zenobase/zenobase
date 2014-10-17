package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Volume;
import javax.measure.unit.Dimension;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.junit.Test;

public class UnitsTest {

	@Test
	public void testDimensions() {
		assertThat(Measures.getUnits(Dimension.LENGTH, Length.class)).as("length units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.MASS, Mass.class)).as("mass units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.TEMPERATURE, Temperature.class)).as("temperature units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.NONE, Dimensionless.class)).as("dimensionless units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.LENGTH.pow(3), Volume.class)).as("volume units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.TIME, Duration.class)).as("time units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.NONE.divide(Dimension.TIME), Frequency.class)).as("frequency units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.LENGTH.pow(3), Volume.class)).as("volume units").isNotEmpty();
	}

	@Test
	public void testIsMetric() {
		assertThat(Units.isMetric(SI.METER)).as("m are metric").isTrue();
		assertThat(Units.isMetric(SI.KILOMETER)).as("km are metric").isTrue();
		assertThat(Units.isMetric(SI.CENTIMETER)).as("cm are metric").isTrue();
		assertThat(Units.isMetric(NonSI.FOOT)).as("ft are metric").isFalse();
		assertThat(Units.isMetric(NonSI.MILE)).as("mi are metric").isFalse();
	}
}
