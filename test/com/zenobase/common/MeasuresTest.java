package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Volume;
import javax.measure.unit.Dimension;

import org.junit.Test;

public class MeasuresTest {

	@Test
	public void testUnits() {
		assertThat(Measures.getUnits(Dimension.LENGTH, Length.class)).as("length units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.MASS, Mass.class)).as("mass units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.TEMPERATURE, Temperature.class)).as("temperature units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.NONE, Dimensionless.class)).as("dimensionless units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.LENGTH.pow(3), Volume.class)).as("volume units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.TIME, Duration.class)).as("time units").isNotEmpty();
	}

	@Test
	public void testToStandard() {
		assertMeasureEqual("16093.44 m", "10 mi");
		assertMeasureEqual("10000 m", "10 km");
		assertMeasureEqual("68.0388555 kg", "150 lb");
		assertMeasureEqual("294.2611111111112 K", "70 °F");
		assertMeasureEqual("293.15 K", "20 °C");
		assertMeasureEqual("0.001 m³", "1 L");
		assertMeasureEqual("1 m³", "1 m³");
	}

	private void assertMeasureEqual(String expected, String value) {
		assertThat(Measures.toStandard(DecimalMeasure.valueOf(value))).isEqualTo(DecimalMeasure.valueOf(expected));
	}
}
