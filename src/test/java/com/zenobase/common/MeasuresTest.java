package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import javax.measure.DecimalMeasure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class MeasuresTest {

	@ParameterizedTest
	@CsvSource({
		"1.00 m, 1.00 m",
		"0.3048 m, 1 ft",
		"0.9144 m, 1 yd",
		"1609.344 m, 1 mi",
		"1000 m, 1 km",
		"0.01 m, 1 cm",
		"0.001 m, 1 mm",
	})
	public void testLength(String expected, String value) {
		assertThatIsEqualTo(expected, value);
	}

	@ParameterizedTest
	@CsvSource({
		"1 kg, 1 kg",
		"0.4535924 kg, 1 lb",
		"0.02834952 kg, 1 oz",
		"6.350294 kg, 1 st",
		"0.001 kg, 1 g",
		"0.000001 kg, 1 mg",
	})
	public void testMass(String expected, String value) {
		assertThatIsEqualTo(expected, value);
	}

	@ParameterizedTest
	@CsvSource({
		"294 K, 294 K",
		"294.2611111111112 K, 70 F",
		"294.2611111111112 K, 70 °F",
		"293.15 K, 20 C",
		"293.15 K, 20 °C",
	})
	public void testTemperature(String expected, String value) {
		assertThatIsEqualTo(expected, value);
	}

	@ParameterizedTest
	@CsvSource({
		"1 m^3, 1 m^3",
		"0.001 m^3, 1 L",
		"0.0001 m^3, 1 dL",
		"0.00001 m^3, 1 cL",
		"0.000001 m^3, 1 mL",
		"0.003785412 m^3, 1 gal",
		"0.000946353 m^3, 1 qt",
		"0.0004731765 m^3, 1 pt",
		"0.0002365882 m^3, 1 cups",
		"0.00002957353 m^3, 1 fl_oz",
	})
	public void testVolume(String expected, String value) {
		assertThatIsEqualTo(expected, value);
	}

	@ParameterizedTest
	@CsvSource({
		"1.0 kg/m^3, 1 g/L",
		"0.001 kg/m^3, 1 mg/L",
		"0.000001 kg/m^3, 1 ug/L",
		"0.000000001 kg/m^3, 1 ng/L",
		"0.000000000001 kg/m^3, 1 pg/L",
		"10 kg/m^3, 1 g/dL",
		"0.01 kg/m^3, 1 mg/dL",
		"0.00001 kg/m^3, 1 ug/dL",
		"0.00000001 kg/m^3, 1 ng/dL",
		"0.00000000001 kg/m^3, 1 pg/dL",
		"1000 kg/m^3, 1 g/mL",
		"1.0 kg/m^3, 1 mg/mL",
		"0.001 kg/m^3, 1 ug/mL",
		"0.000001 kg/m^3, 1 ng/mL",
		"0.000000001 kg/m^3, 1 pg/mL",
	})
	public void testConcentration(String expected, String value) {
		assertThatIsEqualTo(expected, value);
	}

	@ParameterizedTest
	@CsvSource({ "1 Hz, 1 Hz", "1000 Hz, 1 kHz", "1 Hz, 60 bpm", "1 Hz, 60 rpm" })
	public void testFrequency(String expected, String value) {
		assertThatIsEqualTo(expected, value);
	}

	@ParameterizedTest
	@CsvSource({
		"1 Pa, 1 Pa",
		"100 Pa, 1 hPa",
		"1000 Pa, 1 kPa",
		"100 Pa, 1 mbar",
		"100000 Pa, 1 bar",
		"133.322 Pa, 1 mmHg",
		"3386.388 Pa, 1 inHg",
		"6894.757 Pa, 1 psi",
		"98.0665 Pa, 1 cm_wg",
	})
	public void testPressure(String expected, String value) {
		assertThatIsEqualTo(expected, value);
	}

	@ParameterizedTest
	@CsvSource({
		"1 m/s, 1 m/s",
		"0.2777778 m/s, 1 kmh",
		"0.44704 m/s, 1 mph",
		"0.5144444 m/s, 1 kn",
		"331.6 m/s, 1 Mach",
	})
	public void testVelocity(String expected, String value) {
		assertThatIsEqualTo(expected, value);
	}

	@ParameterizedTest
	@CsvSource({ "1 s/m, 1 s/m", "0.001 s/m, 1 s/km", "0.0006213712 s/m, 1 s/mi" })
	public void testPace(String expected, String value) {
		assertThatIsEqualTo(expected, value);
	}

	@ParameterizedTest
	@CsvSource({
		"1 bit, 1 bit",
		"8 bit, 1 B",
		"8000 bit, 1 KB",
		"8000000 bit, 1 MB",
		"8.000000E+9 bit, 1 GB",
		"8.000000E+12 bit, 1 TB",
		"8.000000E+15 bit, 1 PB",
		"8192 bit, 1 KiB",
		"8388608 bit, 1 MiB",
		"8.589935E+9 bit, 1 GiB",
		"8.796093E+12 bit, 1 TiB",
		"9.007199E+15 bit, 1 PiB",
	})
	public void testBits(String expected, String value) {
		assertThatIsEqualTo(expected, value);
	}

	@ParameterizedTest
	@CsvSource({ "1 J, 1 J", "1000 J, 1 kJ", "4.184000 J, 1 cal", "4184.000 J, 1 kcal" })
	public void testEnergy(String expected, String value) {
		assertThatIsEqualTo(expected, value);
	}

	@ParameterizedTest
	@CsvSource({ "1000000 m^-2, 1 kpl", "1.0E+6 m^-2, 2.35214583 mpg" })
	public void testDistancePerVolume(String expected, String value) {
		assertThatIsEqualTo(expected, value);
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
