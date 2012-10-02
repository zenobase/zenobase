package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Volume;
import javax.measure.unit.Dimension;

import org.junit.Test;

public class MeasuresTest {

	@Test
	public void testDimensions() {
		assertThat(Measures.getUnits(Dimension.LENGTH, Length.class)).as("length units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.MASS, Mass.class)).as("mass units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.TEMPERATURE, Temperature.class)).as("temperature units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.NONE, Dimensionless.class)).as("dimensionless units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.LENGTH.pow(3), Volume.class)).as("volume units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.TIME, Duration.class)).as("time units").isNotEmpty();
		assertThat(Measures.getUnits(Dimension.NONE.divide(Dimension.TIME), Frequency.class)).as("frequency units").isNotEmpty();
	}

	@Test
	public void testLength() {
		assertThatIsEqualTo("0.3048 m", "1 ft");
		assertThatIsEqualTo("1609.344 m", "1 mi");
		assertThatIsEqualTo("1000 m", "1 km");
		assertThatIsEqualTo("0.01 m", "1 cm");
		assertThatIsEqualTo("0.001 m", "1 mm");
	}

	@Test
	public void testMass() {
		assertThatIsEqualTo("0.4535924 kg", "1 lb");
		assertThatIsEqualTo("0.02834952 kg", "1 oz");
		assertThatIsEqualTo("0.001 kg", "1 g");
		assertThatIsEqualTo("0.000001 kg", "1 mg");
	}

	@Test
	public void testTemperature() {
		assertThatIsEqualTo("294.2611111111112 K", "70 F");
		assertThatIsEqualTo("294.2611111111112 K", "70 °F");
		assertThatIsEqualTo("293.15 K", "20 C");
		assertThatIsEqualTo("293.15 K", "20 °C");
	}

	@Test
	public void testVolume() {
		assertThatIsEqualTo("0.001 m³", "1 L");
		assertThatIsEqualTo("1 m³", "1 m³");
	}

	@Test
	public void testFrequency() {
		assertThatIsEqualTo("1000 Hz", "1 kHz");
		assertThatIsEqualTo("1 Hz", "60 bpm");
	}

	@Test
	public void testPressure() {
		assertThatIsEqualTo("133.322 Pa", "1 mmHg");
		assertThatIsEqualTo("3386.388 Pa", "1 inHg");
		assertThatIsEqualTo("6894.757 Pa", "1 psi");
	}

	@Test
	public void testSpeed() {
		assertThatIsEqualTo("0.2777778 m/s", "1 kmh");
		assertThatIsEqualTo("0.44704 m/s", "1 mph");
		assertThatIsEqualTo("0.5144444 m/s", "1 kn");
		assertThatIsEqualTo("331.6 m/s", "1 Mach");
	}

	private static void assertThatIsEqualTo(String expected, String value) {
		assertThat(Measures.toStandard(DecimalMeasure.valueOf(value))).isEqualTo(DecimalMeasure.valueOf(expected));
	}
}
