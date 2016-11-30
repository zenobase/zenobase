package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;

import org.junit.Test;

public class MeasuresTest {

	@Test
	public void testLength() {
		assertThatIsEqualTo("1.00 m", "1.00 m");
		assertThatIsEqualTo("0.3048 m", "1 ft");
		assertThatIsEqualTo("0.9144 m", "1 yd");
		assertThatIsEqualTo("1609.344 m", "1 mi");
		assertThatIsEqualTo("1000 m", "1 km");
		assertThatIsEqualTo("0.01 m", "1 cm");
		assertThatIsEqualTo("0.001 m", "1 mm");
	}

	@Test
	public void testMass() {
		assertThatIsEqualTo("1 kg", "1 kg");
		assertThatIsEqualTo("0.4535924 kg", "1 lb");
		assertThatIsEqualTo("0.02834952 kg", "1 oz");
		assertThatIsEqualTo("6.350294 kg", "1 st");
		assertThatIsEqualTo("0.001 kg", "1 g");
		assertThatIsEqualTo("0.000001 kg", "1 mg");
	}

	@Test
	public void testTemperature() {
		assertThatIsEqualTo("294 K", "294 K");
		assertThatIsEqualTo("294.2611111111112 K", "70 F");
		assertThatIsEqualTo("294.2611111111112 K", "70 °F");
		assertThatIsEqualTo("293.15 K", "20 C");
		assertThatIsEqualTo("293.15 K", "20 °C");
	}

	@Test
	public void testVolume() {
		assertThatIsEqualTo("1 m^3", "1 m^3");
		assertThatIsEqualTo("0.001 m^3", "1 L");
		assertThatIsEqualTo("0.0001 m^3", "1 dL");
		assertThatIsEqualTo("0.00001 m^3", "1 cL");
		assertThatIsEqualTo("0.000001 m^3", "1 mL");
		assertThatIsEqualTo("0.003785412 m^3", "1 gal");
		assertThatIsEqualTo("0.000946353 m^3", "1 qt");
		assertThatIsEqualTo("0.0004731765 m^3", "1 pt");
		assertThatIsEqualTo("0.0002365882 m^3", "1 cups");
		assertThatIsEqualTo("0.00002957353 m^3", "1 fl_oz");
	}

	@Test
	public void testConcentration() {
		assertThatIsEqualTo("1.0 kg/m^3", "1 g/L");
		assertThatIsEqualTo("0.001 kg/m^3", "1 mg/L");
		assertThatIsEqualTo("0.000001 kg/m^3", "1 ug/L");
		assertThatIsEqualTo("0.000000001 kg/m^3", "1 ng/L");
		assertThatIsEqualTo("10 kg/m^3", "1 g/dL");
		assertThatIsEqualTo("0.01 kg/m^3", "1 mg/dL");
		assertThatIsEqualTo("0.00001 kg/m^3", "1 ug/dL");
		assertThatIsEqualTo("0.00000001 kg/m^3", "1 ng/dL");
		assertThatIsEqualTo("1000 kg/m^3", "1 g/mL");
		assertThatIsEqualTo("1.0 kg/m^3", "1 mg/mL");
		assertThatIsEqualTo("0.001 kg/m^3", "1 ug/mL");
		assertThatIsEqualTo("0.000001 kg/m^3", "1 ng/mL");
	}

	@Test
	public void testFrequency() {
		assertThatIsEqualTo("1 Hz", "1 Hz");
		assertThatIsEqualTo("1000 Hz", "1 kHz");
		assertThatIsEqualTo("1 Hz", "60 bpm");
		assertThatIsEqualTo("1 Hz", "60 rpm");
	}

	@Test
	public void testPressure() {
		assertThatIsEqualTo("1 Pa", "1 Pa");
		assertThatIsEqualTo("100 Pa", "1 hPa");
		assertThatIsEqualTo("1000 Pa", "1 kPa");
		assertThatIsEqualTo("100 Pa", "1 mbar");
		assertThatIsEqualTo("100000 Pa", "1 bar");
		assertThatIsEqualTo("133.322 Pa", "1 mmHg");
		assertThatIsEqualTo("3386.388 Pa", "1 inHg");
		assertThatIsEqualTo("6894.757 Pa", "1 psi");
        assertThatIsEqualTo("98.0665 Pa", "1 cm_wg");
	}

	@Test
	public void testVelocity() {
		assertThatIsEqualTo("1 m/s", "1 m/s");
		assertThatIsEqualTo("0.2777778 m/s", "1 kmh");
		assertThatIsEqualTo("0.44704 m/s", "1 mph");
		assertThatIsEqualTo("0.5144444 m/s", "1 kn");
		assertThatIsEqualTo("331.6 m/s", "1 Mach");
	}

	@Test
	public void testPace() {
		assertThatIsEqualTo("1 s/m", "1 s/m");
		assertThatIsEqualTo("0.001 s/m", "1 s/km");
		assertThatIsEqualTo("0.0006213712 s/m", "1 s/mi");
	}

	@Test
	public void testBits() {
		assertThatIsEqualTo("1 bit", "1 bit");
		assertThatIsEqualTo("8 bit", "1 B");
		assertThatIsEqualTo("8000 bit", "1 KB");
		assertThatIsEqualTo("8000000 bit", "1 MB");
		assertThatIsEqualTo("8.000000E+9 bit", "1 GB");
		assertThatIsEqualTo("8.000000E+12 bit", "1 TB");
		assertThatIsEqualTo("8.000000E+15 bit", "1 PB");
		assertThatIsEqualTo("8192 bit", "1 KiB");
		assertThatIsEqualTo("8388608 bit", "1 MiB");
		assertThatIsEqualTo("8.589935E+9 bit", "1 GiB");
		assertThatIsEqualTo("8.796093E+12 bit", "1 TiB");
		assertThatIsEqualTo("9.007199E+15 bit", "1 PiB");
	}

	@Test
	public void testEnergy() {
		assertThatIsEqualTo("1 J", "1 J");
		assertThatIsEqualTo("1000 J", "1 kJ");
		assertThatIsEqualTo("4.184000 J", "1 cal");
		assertThatIsEqualTo("4184.000 J", "1 kcal");
	}

	@Test
	public void testDistancePerVolume() {
		assertThatIsEqualTo("1000000 m^-2", "1 kpl");
		assertThatIsEqualTo("1.0E+6 m^-2", "2.35214583 mpg");
	}

	@Test
	public void testSound() {
		assertThatIsEqualTo("42 dB", "42 dB");
	}

	@Test
	public void testLight() {
		assertThatIsEqualTo("1 lx", "1 lx");
	}

	private static void assertThatIsEqualTo(String expected, String value) {
		assertThat(Measures.toStandard(DecimalMeasure.valueOf(value))).isEqualTo(DecimalMeasure.valueOf(expected));
	}

	@Test
	public void testRounding() {
		assertThat(Measures.round(1.665)).isEqualTo(new BigDecimal("1.67"));
		assertThat(Measures.round(Double.NaN)).isNull();
	}
}
