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

import org.junit.Test;

public class UnitsTest {

	@Test
	public void testDimensions() {
		assertThat(Units.getUnits(Dimension.LENGTH, Length.class)).as("length units").isNotEmpty();
		assertThat(Units.getUnits(Dimension.MASS, Mass.class)).as("mass units").isNotEmpty();
		assertThat(Units.getUnits(Dimension.TEMPERATURE, Temperature.class)).as("temperature units").isNotEmpty();
		assertThat(Units.getUnits(Dimension.NONE, Dimensionless.class)).as("dimensionless units").isNotEmpty();
		assertThat(Units.getUnits(Dimension.LENGTH.pow(3), Volume.class)).as("volume units").isNotEmpty();
		assertThat(Units.getUnits(Dimension.TIME, Duration.class)).as("time units").isNotEmpty();
		assertThat(Units.getUnits(Dimension.NONE.divide(Dimension.TIME), Frequency.class)).as("frequency units").isNotEmpty();
		assertThat(Units.getUnits(Dimension.LENGTH.pow(3), Volume.class)).as("volume units").isNotEmpty();
	}

	@Test
	public void testIsMetric() {
		assertThat(Units.isMetric(Units.M)).as("m are metric").isTrue();
		assertThat(Units.isMetric(Units.KM)).as("km are metric").isTrue();
		assertThat(Units.isMetric(Units.CM)).as("cm are metric").isTrue();
		assertThat(Units.isMetric(Units.FT)).as("ft are metric").isFalse();
		assertThat(Units.isMetric(Units.YD)).as("yd are metric").isFalse();
		assertThat(Units.isMetric(Units.MI)).as("mi are metric").isFalse();
	}
}
