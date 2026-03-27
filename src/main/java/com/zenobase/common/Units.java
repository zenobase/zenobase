package com.zenobase.common;

import java.util.ArrayList;
import java.util.List;
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
import javax.measure.unit.Dimension;
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

	public static final Unit<DataAmount> BIT = valueOf("bit");
	public static final Unit<DataAmount> B = valueOf("B");
	public static final Unit<DataAmount> KB = valueOf("KB");
	public static final Unit<DataAmount> MB = valueOf("MB");
	public static final Unit<DataAmount> GB = valueOf("GB");
	public static final Unit<DataAmount> TB = valueOf("TB");
	public static final Unit<DataAmount> PB = valueOf("PB");
	public static final Unit<DataAmount> KIB = valueOf("KiB");
	public static final Unit<DataAmount> MIB = valueOf("MiB");
	public static final Unit<DataAmount> GIB = valueOf("GiB");
	public static final Unit<DataAmount> TIB = valueOf("TiB");
	public static final Unit<DataAmount> PIB = valueOf("PiB");

	public static final Unit<VolumetricDensity> G_PER_L = valueOf("g/L");
	public static final Unit<VolumetricDensity> MG_PER_L = valueOf("mg/L");
	public static final Unit<VolumetricDensity> UG_PER_L = valueOf("ug/L");
	public static final Unit<VolumetricDensity> NG_PER_L = valueOf("ng/L");
	public static final Unit<VolumetricDensity> PG_PER_L = valueOf("pg/L");
	public static final Unit<VolumetricDensity> G_PER_DL = valueOf("g/dL");
	public static final Unit<VolumetricDensity> MG_PER_DL = valueOf("mg/dL");
	public static final Unit<VolumetricDensity> UG_PER_DL = valueOf("ug/dL");
	public static final Unit<VolumetricDensity> NG_PER_DL = valueOf("ng/dL");
	public static final Unit<VolumetricDensity> PG_PER_DL = valueOf("pg/dL");
	public static final Unit<VolumetricDensity> G_PER_ML = valueOf("g/mL");
	public static final Unit<VolumetricDensity> MG_PER_ML = valueOf("mg/mL");
	public static final Unit<VolumetricDensity> UG_PER_ML = valueOf("ug/mL");
	public static final Unit<VolumetricDensity> NG_PER_ML = valueOf("ng/mL");
	public static final Unit<VolumetricDensity> PG_PER_ML = valueOf("pg/mL");

	public static final Unit<Length> MM = valueOf("mm");
	public static final Unit<Length> CM = valueOf("cm");
	public static final Unit<Length> M = valueOf("m");
	public static final Unit<Length> KM = valueOf("km");
	public static final Unit<Length> IN = valueOf("in");
	public static final Unit<Length> FT = valueOf("ft");
	public static final Unit<Length> YD = valueOf("yd");
	public static final Unit<Length> MI = valueOf("mi");

	public static final Unit<LengthPerVolume> KPL = valueOf("kpl");
	public static final Unit<LengthPerVolume> MPG = valueOf("mpg");

	public static final Unit<Energy> J = valueOf("J");
	public static final Unit<Energy> KJ = valueOf("kJ");
	public static final Unit<Energy> CAL = valueOf("cal");
	public static final Unit<Energy> KCAL = valueOf("kcal");
	public static final Unit<Energy> KWH = valueOf("kWh");

	public static final Unit<Frequency> BPM = valueOf("bpm");
	public static final Unit<Frequency> RPM = valueOf("rpm");
	public static final Unit<Frequency> HZ = valueOf("Hz");

	public static final Unit<Pressure> PA = valueOf("Pa");
	public static final Unit<Pressure> HPA = valueOf("hPa");
	public static final Unit<Pressure> KPA = valueOf("kPa");
	public static final Unit<Pressure> MMHG = valueOf("mmHg");
	public static final Unit<Pressure> INHG = valueOf("inHg");
	public static final Unit<Pressure> PSI = valueOf("psi");
	public static final Unit<Pressure> CM_WG = valueOf("cm_wg");

	public static final Unit<Dimensionless> DB = valueOf("dB");

	public static final Unit<Temperature> K = valueOf("K");
	public static final Unit<Temperature> C = valueOf("C");
	public static final Unit<Temperature> F = valueOf("F");

	public static final Unit<Velocity> M_PER_S = valueOf("m/s");
	public static final Unit<Velocity> KMH = valueOf("kmh");
	public static final Unit<Velocity> MPH = valueOf("mph");
	public static final Unit<Velocity> KN = valueOf("kn");
	public static final Unit<Velocity> MACH = valueOf("Mach");

	public static final Unit<Pace> S_PER_KM = valueOf("s/km");
	public static final Unit<Pace> S_PER_MI = valueOf("s/mi");

	public static final Unit<Volume> ML = valueOf("mL");
	public static final Unit<Volume> CL = valueOf("cL");
	public static final Unit<Volume> DL = valueOf("dL");
	public static final Unit<Volume> L = valueOf("L");
	public static final Unit<Volume> FL_OZ = valueOf("fl_oz");
	public static final Unit<Volume> PT = valueOf("pt");
	public static final Unit<Volume> QT = valueOf("qt");
	public static final Unit<Volume> GAL = valueOf("gal");

	public static final Unit<Mass> NG = valueOf("ng");
	public static final Unit<Mass> UG = valueOf("ug");
	public static final Unit<Mass> MG = valueOf("mg");
	public static final Unit<Mass> G = valueOf("g");
	public static final Unit<Mass> KG = valueOf("kg");
	public static final Unit<Mass> OZ = valueOf("oz");
	public static final Unit<Mass> LB = valueOf("lb");
	public static final Unit<Mass> ST = valueOf("st");

	public static final Unit<Illuminance> LX = valueOf("lx");

	private Units() {}

	/**
	 * Obtain units from here to ensure the static initializers have registered any custom units.
	 */
	@SuppressWarnings("unchecked")
	public static <Q extends Quantity> Unit<Q> valueOf(String s) {
		return (Unit<Q>) Unit.valueOf(s);
	}

	public static <Q extends Quantity> boolean isMetric(Unit<Q> unit) {
		return unit.isStandardUnit()
				|| Math.log10(unit.getConverterTo(unit.getStandardUnit()).convert(1)) % 1 == 0;
	}

	public static boolean isStandard(Unit<?> unit) {
		return unit.isStandardUnit() || DB.equals(unit);
	}

	public static <Q extends Quantity> List<Unit<Q>> getUnits(Dimension dimension, Class<Q> type) {
		List<Unit<Q>> units = new ArrayList<>();
		for (Unit<?> unit : SI.getInstance().getUnits()) {
			if (unit.getDimension().equals(dimension)) {
				units.add(unit.asType(type));
			}
		}
		for (Unit<?> unit : NonSI.getInstance().getUnits()) {
			if (unit.getDimension().equals(dimension)) {
				units.add(unit.asType(type));
			}
		}
		return units;
	}
}
