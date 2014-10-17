package com.zenobase.common;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Quantity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

public class Units {

	static {

		UnitFormat.getInstance().label(SI.CELSIUS, "C");
		UnitFormat.getInstance().alias(SI.CELSIUS, "°C");
		UnitFormat.getInstance().label(NonSI.FAHRENHEIT, "F");
		UnitFormat.getInstance().alias(NonSI.FAHRENHEIT, "°F");

		UnitFormat.getInstance().label(SI.HERTZ.divide(60L), "bpm");
		UnitFormat.getInstance().alias(SI.HERTZ.divide(60L), "rpm");

		UnitFormat.getInstance().label(NonSI.OUNCE, "oz");

		UnitFormat.getInstance().label(SI.PASCAL.times(6894.75729), "psi");

		UnitFormat.getInstance().alias(NonSI.KILOMETERS_PER_HOUR, "kmh");

		UnitFormat.getInstance().label(NonSI.OUNCE_LIQUID_US, "fl_oz");
		UnitFormat.getInstance().label(NonSI.OUNCE_LIQUID_US.times(8), "cups");
		UnitFormat.getInstance().label(NonSI.OUNCE_LIQUID_US.times(16), "pt");
		UnitFormat.getInstance().label(NonSI.OUNCE_LIQUID_US.times(32), "qt");

		UnitFormat.getInstance().label(SI.MICRO(SI.GRAM), "ug");
		UnitFormat.getInstance().label(NonSI.POUND.times(14), "st");

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

		UnitFormat.getInstance().label(SI.JOULE.divide(0.239005736), "cal");
		UnitFormat.getInstance().label(SI.JOULE.divide(0.000239005736), "kcal");

		//UnitFormat.getInstance().label(SI.METER.pow(-2), "m^-2");
		UnitFormat.getInstance().label(SI.KILOMETER.divide(NonSI.LITER), "kpl");
		UnitFormat.getInstance().label(NonSI.MILE.divide(NonSI.GALLON_LIQUID_US), "mpg");
	}

	public static final Unit<Energy> KCAL = valueOf("kcal");

	private Units() {

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
}
