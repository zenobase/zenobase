package com.zenobase.common;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.measure.quantity.DataAmount;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Illuminance;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Quantity;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Velocity;
import javax.measure.quantity.Volume;
import javax.measure.quantity.VolumetricDensity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

public class Units {

	static {
		UnitFormat.getInstance().label(SI.BIT.times(8), "B");
		UnitFormat.getInstance().label(SI.KILO(SI.BIT.times(8)), "KB");
		UnitFormat.getInstance().label(SI.MEGA(SI.BIT.times(8)), "MB");
		UnitFormat.getInstance().label(SI.GIGA(SI.BIT.times(8)), "GB");
		UnitFormat.getInstance().label(SI.TERA(SI.BIT.times(8)), "TB");
		UnitFormat.getInstance().label(SI.PETA(SI.BIT.times(8)), "PB");
		UnitFormat.getInstance().label(SI.BIT.times(8L * 1024), "KiB");
		UnitFormat.getInstance().label(SI.BIT.times(8L * 1024 * 1024), "MiB");
		UnitFormat.getInstance().label(SI.BIT.times(8L * 1024 * 1024 * 1024), "GiB");
		UnitFormat.getInstance().label(SI.BIT.times(8L * 1024 * 1024 * 1024 * 1024), "TiB");
		UnitFormat.getInstance().label(SI.BIT.times(8L * 1024 * 1024 * 1024 * 1024 * 1024), "PiB");

		UnitFormat.getInstance().label(SI.KILOMETER.divide(NonSI.LITER), "kpl");
		UnitFormat.getInstance().label(NonSI.MILE.divide(NonSI.GALLON_LIQUID_US), "mpg");

		UnitFormat.getInstance().label(SI.JOULE.divide(0.239005736), "cal");
		UnitFormat.getInstance().label(SI.JOULE.divide(0.000239005736), "kcal");
		UnitFormat.getInstance().label(SI.JOULE.times(3600000), "kWh");

		UnitFormat.getInstance().label(SI.HERTZ.divide(60L), "bpm");
		UnitFormat.getInstance().alias(SI.HERTZ.divide(60L), "rpm");

		UnitFormat.getInstance().label(SI.PASCAL.times(6894.75729), "psi");
		UnitFormat.getInstance().alias(SI.HECTO(SI.PASCAL), "mbar");
		UnitFormat.getInstance().alias(SI.HECTO(SI.PASCAL).times(1000), "bar");
		UnitFormat.getInstance().label(SI.PASCAL.times(98.0665), "cm_wg");

		UnitFormat.getInstance().label(SI.CELSIUS, "C");
		UnitFormat.getInstance().label(NonSI.FAHRENHEIT, "F");

		UnitFormat.getInstance().alias(NonSI.KILOMETERS_PER_HOUR, "kmh");

		// UnitFormat.getInstance().label(SI.MILLI(SI.SECOND).divide(SI.KILOMETER), "ms/km");
		// UnitFormat.getInstance().label(SI.MILLI(SI.SECOND).divide(NonSI.MILE), "ms/mi");

		UnitFormat.getInstance().label(NonSI.OUNCE_LIQUID_US, "fl_oz");
		UnitFormat.getInstance().label(NonSI.OUNCE_LIQUID_US.times(8), "cups");
		UnitFormat.getInstance().label(NonSI.OUNCE_LIQUID_US.times(16), "pt");
		UnitFormat.getInstance().label(NonSI.OUNCE_LIQUID_US.times(32), "qt");

		UnitFormat.getInstance().label(NonSI.OUNCE, "oz");
		UnitFormat.getInstance().label(SI.MICRO(SI.GRAM), "ug");
		UnitFormat.getInstance().label(NonSI.POUND.times(14), "st");
	}

	private static final Set<String> KNOWN = new HashSet<>();

	public static final Unit<DataAmount> BIT = known("bit");
	public static final Unit<DataAmount> B = known("B");
	public static final Unit<DataAmount> KB = known("KB");
	public static final Unit<DataAmount> MB = known("MB");
	public static final Unit<DataAmount> GB = known("GB");
	public static final Unit<DataAmount> TB = known("TB");
	public static final Unit<DataAmount> PB = known("PB");
	public static final Unit<DataAmount> KIB = known("KiB");
	public static final Unit<DataAmount> MIB = known("MiB");
	public static final Unit<DataAmount> GIB = known("GiB");
	public static final Unit<DataAmount> TIB = known("TiB");
	public static final Unit<DataAmount> PIB = known("PiB");

	public static final Unit<VolumetricDensity> G_PER_L = known("g/L");
	public static final Unit<VolumetricDensity> MG_PER_L = known("mg/L");
	public static final Unit<VolumetricDensity> UG_PER_L = known("ug/L");
	public static final Unit<VolumetricDensity> NG_PER_L = known("ng/L");
	public static final Unit<VolumetricDensity> PG_PER_L = known("pg/L");
	public static final Unit<VolumetricDensity> G_PER_DL = known("g/dL");
	public static final Unit<VolumetricDensity> MG_PER_DL = known("mg/dL");
	public static final Unit<VolumetricDensity> UG_PER_DL = known("ug/dL");
	public static final Unit<VolumetricDensity> NG_PER_DL = known("ng/dL");
	public static final Unit<VolumetricDensity> PG_PER_DL = known("pg/dL");
	public static final Unit<VolumetricDensity> G_PER_ML = known("g/mL");
	public static final Unit<VolumetricDensity> MG_PER_ML = known("mg/mL");
	public static final Unit<VolumetricDensity> UG_PER_ML = known("ug/mL");
	public static final Unit<VolumetricDensity> NG_PER_ML = known("ng/mL");
	public static final Unit<VolumetricDensity> PG_PER_ML = known("pg/mL");

	public static final Unit<Length> MM = known("mm");
	public static final Unit<Length> CM = known("cm");
	public static final Unit<Length> M = known("m");
	public static final Unit<Length> KM = known("km");
	public static final Unit<Length> IN = known("in");
	public static final Unit<Length> FT = known("ft");
	public static final Unit<Length> YD = known("yd");
	public static final Unit<Length> MI = known("mi");

	public static final Unit<LengthPerVolume> KPL = known("kpl");
	public static final Unit<LengthPerVolume> MPG = known("mpg");

	public static final Unit<Energy> J = known("J");
	public static final Unit<Energy> KJ = known("kJ");
	public static final Unit<Energy> CAL = known("cal");
	public static final Unit<Energy> KCAL = known("kcal");
	public static final Unit<Energy> KWH = known("kWh");

	public static final Unit<Frequency> BPM = known("bpm");
	public static final Unit<Frequency> RPM = known("rpm");
	public static final Unit<Frequency> HZ = known("Hz");

	public static final Unit<Pressure> PA = known("Pa");
	public static final Unit<Pressure> HPA = known("hPa");
	public static final Unit<Pressure> KPA = known("kPa");
	public static final Unit<Pressure> MMHG = known("mmHg");
	public static final Unit<Pressure> INHG = known("inHg");
	public static final Unit<Pressure> PSI = known("psi");
	public static final Unit<Pressure> CM_WG = known("cm_wg");

	public static final Unit<Dimensionless> DB = known("dB");

	public static final Unit<Temperature> K = known("K");
	public static final Unit<Temperature> C = known("C");
	public static final Unit<Temperature> F = known("F");

	public static final Unit<Velocity> M_PER_S = known("m/s");
	public static final Unit<Velocity> KMH = known("kmh");
	public static final Unit<Velocity> MPH = known("mph");
	public static final Unit<Velocity> KN = known("kn");
	public static final Unit<Velocity> MACH = known("Mach");

	public static final Unit<Pace> S_PER_KM = known("s/km");
	public static final Unit<Pace> S_PER_MI = known("s/mi");

	public static final Unit<Volume> ML = known("mL");
	public static final Unit<Volume> CL = known("cL");
	public static final Unit<Volume> DL = known("dL");
	public static final Unit<Volume> L = known("L");
	public static final Unit<Volume> FL_OZ = known("fl_oz");
	public static final Unit<Volume> CUPS = known("cups");
	public static final Unit<Volume> PT = known("pt");
	public static final Unit<Volume> QT = known("qt");
	public static final Unit<Volume> GAL = known("gal");

	public static final Unit<Mass> NG = known("ng");
	public static final Unit<Mass> UG = known("ug");
	public static final Unit<Mass> MG = known("mg");
	public static final Unit<Mass> G = known("g");
	public static final Unit<Mass> KG = known("kg");
	public static final Unit<Mass> OZ = known("oz");
	public static final Unit<Mass> LB = known("lb");
	public static final Unit<Mass> ST = known("st");

	public static final Unit<Illuminance> LX = known("lx");

	static {
		for (String alias : List.of("mbar", "bar", "°C", "°F")) {
			known(alias);
		}
	}

	private Units() {}

	/**
	 * Registers the symbol as accepted for new events, along with its canonical form (e.g. "kmh" and "km/h").
	 */
	private static <Q extends Quantity> Unit<Q> known(String s) {
		Unit<Q> unit = valueOf(s);
		KNOWN.add(s);
		KNOWN.add(unit.toString());
		return unit;
	}

	/**
	 * Whether new events may use this symbol. Stored events may contain others that {@link #valueOf} still parses.
	 */
	public static boolean isKnown(String symbol) {
		return KNOWN.contains(symbol);
	}

	/**
	 * Obtain units from here to ensure the static initializers have registered any custom units.
	 */
	@SuppressWarnings("unchecked")
	public static <Q extends Quantity> Unit<Q> valueOf(String s) {
		return (Unit<Q>) Unit.valueOf(s);
	}

	public static <Q extends Quantity> boolean isMetric(Unit<Q> unit) {
		return unit.isStandardUnit() || Math.log10(unit.getConverterTo(unit.getStandardUnit()).convert(1)) % 1 == 0;
	}

	public static boolean isStandard(Unit<?> unit) {
		return unit.isStandardUnit() || DB.equals(unit);
	}

	public static boolean isDimensionless(Unit<?> unit) {
		return Unit.ONE.equals(unit);
	}
}
