package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.Unit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class UnitsTest {

	@Test
	public void testIsMetric() {
		assertThat(Units.isMetric(Units.M)).as("m are metric").isTrue();
		assertThat(Units.isMetric(Units.KM)).as("km are metric").isTrue();
		assertThat(Units.isMetric(Units.CM)).as("cm are metric").isTrue();
		assertThat(Units.isMetric(Units.FT)).as("ft are metric").isFalse();
		assertThat(Units.isMetric(Units.YD)).as("yd are metric").isFalse();
		assertThat(Units.isMetric(Units.MI)).as("mi are metric").isFalse();
	}

	/**
	 * Pins how every symbol accepted for new events is parsed, formatted and converted to its standard unit, so that
	 * a replacement for jscience can be checked against stored data.
	 */
	@ParameterizedTest
	@CsvSource({
		"bit, bit, bit, 1.0, 0.0",
		"B, B, bit, 8.0, 0.0",
		"KB, KB, bit, 8000.0, 0.0",
		"MB, MB, bit, 8000000.0, 0.0",
		"GB, GB, bit, 8.0E9, 0.0",
		"TB, TB, bit, 8.0E12, 0.0",
		"PB, PB, bit, 8.0E15, 0.0",
		"KiB, KiB, bit, 8192.0, 0.0",
		"MiB, MiB, bit, 8388608.0, 0.0",
		"GiB, GiB, bit, 8.589934592E9, 0.0",
		"TiB, TiB, bit, 8.796093022208E12, 0.0",
		"PiB, PiB, bit, 9.007199254740992E15, 0.0",
		"g/L, g/L, kg/m³, 1.0, 0.0",
		"mg/L, mg/L, kg/m³, 0.001, 0.0",
		"ug/L, ug/L, kg/m³, 1.0E-6, 0.0",
		"ng/L, ng/L, kg/m³, 1.0E-9, 0.0",
		"pg/L, pg/L, kg/m³, 1.0E-12, 0.0",
		"g/dL, g/dL, kg/m³, 10.0, 0.0",
		"mg/dL, mg/dL, kg/m³, 0.01, 0.0",
		"ug/dL, ug/dL, kg/m³, 1.0E-5, 0.0",
		"ng/dL, ng/dL, kg/m³, 1.0E-8, 0.0",
		"pg/dL, pg/dL, kg/m³, 1.0E-11, 0.0",
		"g/mL, g/mL, kg/m³, 1000.0, 0.0",
		"mg/mL, mg/mL, kg/m³, 1.0, 0.0",
		"ug/mL, ug/mL, kg/m³, 0.001, 0.0",
		"ng/mL, ng/mL, kg/m³, 1.0E-6, 0.0",
		"pg/mL, pg/mL, kg/m³, 1.0E-9, 0.0",
		"mm, mm, m, 0.001, 0.0",
		"cm, cm, m, 0.01, 0.0",
		"m, m, m, 1.0, 0.0",
		"km, km, m, 1000.0, 0.0",
		"in, in, m, 0.0254, 0.0",
		"ft, ft, m, 0.3048, 0.0",
		"yd, yd, m, 0.9144, 0.0",
		"mi, mi, m, 1609.344, 0.0",
		"kpl, kpl, 1/m², 1000000.0, 0.0",
		"mpg, mpg, 1/m², 425143.707430272, 0.0",
		"J, J, J, 1.0, 0.0",
		"kJ, kJ, J, 1000.0, 0.0",
		"cal, cal, J, 4.184000002409984, 0.0",
		"kcal, kcal, J, 4184.0000024099845, 0.0",
		"kWh, kWh, J, 3600000.0, 0.0",
		"bpm, bpm, Hz, 0.016666666666666666, 0.0",
		"rpm, bpm, Hz, 0.016666666666666666, 0.0",
		"Hz, Hz, Hz, 1.0, 0.0",
		"Pa, Pa, Pa, 1.0, 0.0",
		"hPa, hPa, Pa, 100.0, 0.0",
		"kPa, kPa, Pa, 1000.0, 0.0",
		"mbar, hPa, Pa, 100.0, 0.0",
		"bar, bar, Pa, 100000.0, 0.0",
		"mmHg, mmHg, Pa, 133.322, 0.0",
		"inHg, inHg, Pa, 3386.388, 0.0",
		"psi, psi, Pa, 6894.75729, 0.0",
		"cm_wg, cm_wg, Pa, 98.0665, 0.0",
		"dB, dB, '', 0.2589254117941673, 1.0",
		"K, K, K, 1.0, 0.0",
		"C, C, K, 1.0, 273.15",
		"°C, C, K, 1.0, 273.15",
		"F, F, K, 0.5555555555555713, 255.3722222222222",
		"°F, F, K, 0.5555555555555713, 255.3722222222222",
		"m/s, m/s, m/s, 1.0, 0.0",
		"kmh, km/h, m/s, 0.2777777777777778, 0.0",
		"km/h, km/h, m/s, 0.2777777777777778, 0.0",
		"mph, mph, m/s, 0.44704, 0.0",
		"kn, kn, m/s, 0.5144444444444445, 0.0",
		"Mach, Mach, m/s, 331.6, 0.0",
		"s/km, s/km, s/m, 0.001, 0.0",
		"s/mi, s/mi, s/m, 6.213711922373339E-4, 0.0",
		"mL, mL, m³, 1.0E-6, 0.0",
		"cL, cL, m³, 1.0E-5, 0.0",
		"dL, dL, m³, 1.0E-4, 0.0",
		"L, L, m³, 0.001, 0.0",
		"fl_oz, fl_oz, m³, 2.95735295625E-5, 0.0",
		"cups, cups, m³, 2.365882365E-4, 0.0",
		"pt, pt, m³, 4.73176473E-4, 0.0",
		"qt, qt, m³, 9.46352946E-4, 0.0",
		"gal, gal, m³, 0.003785411784, 0.0",
		"ng, ng, kg, 1.0E-12, 0.0",
		"ug, ug, kg, 1.0E-9, 0.0",
		"mg, mg, kg, 1.0E-6, 0.0",
		"g, g, kg, 0.001, 0.0",
		"kg, kg, kg, 1.0, 0.0",
		"oz, oz, kg, 0.028349523125, 0.0",
		"lb, lb, kg, 0.45359237, 0.0",
		"st, st, kg, 6.35029318, 0.0",
		"lx, lx, lx, 1.0, 0.0",
	})
	public void testKnownUnits(String symbol, String canonical, String standard, double factor, double offset) {
		assertThat(Units.isKnown(symbol)).as("known").isTrue();
		Unit<?> unit = Units.valueOf(symbol);
		assertThat(unit.toString()).as("canonical").isEqualTo(canonical);
		assertThat(Units.isKnown(canonical)).as("canonical known").isTrue();
		assertThat(unit.getStandardUnit().toString()).as("standard").isEqualTo(standard);
		UnitConverter converter = unit.getConverterTo(unit.getStandardUnit());
		assertThat(converter.convert(0)).as("offset").isEqualTo(offset);
		assertThat(converter.convert(1) - offset)
			.as("factor")
			.isEqualTo(factor);
	}

	/**
	 * Symbols that jscience parses, and so may appear in stored events, but that new events can't use.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "", "%", "s", "min", "h", "kHz", "m^2", "kg*m", "µg", "℃" })
	public void testUnknownUnits(String symbol) {
		assertThat(Units.valueOf(symbol)).isNotNull();
		assertThat(Units.isKnown(symbol)).isFalse();
	}
}
