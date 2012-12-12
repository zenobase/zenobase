package com.zenobase.common;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Dimension;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import com.google.common.collect.Lists;

public class Measures {

	static {

		UnitFormat.getInstance().label(SI.CELSIUS, "C");
		UnitFormat.getInstance().alias(SI.CELSIUS, "°C");
		UnitFormat.getInstance().label(NonSI.FAHRENHEIT, "F");
		UnitFormat.getInstance().alias(NonSI.FAHRENHEIT, "°F");

		UnitFormat.getInstance().label(SI.HERTZ.divide(60L), "bpm");

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
	}

	private Measures() {
		throw new AssertionError();
	}

	public static <Q extends Quantity> DecimalMeasure<Q> toStandard(DecimalMeasure<Q> measure) {
		return measure.to((Unit<Q>) measure.getUnit().getStandardUnit(), MathContext.DECIMAL32);
	}

	public static <Q extends Quantity> Iterable<Unit<Q>> getUnits(Dimension dimension, Class<Q> type) {
		List<Unit<Q>> units = Lists.newArrayList();
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

	public static <Q extends Quantity> DecimalMeasure<Q> valueOf(BigDecimal value, String unit) {
		Unit<Q> u = valueOf(unit);
		return DecimalMeasure.valueOf(value, u);
	}

	/**
	 * Obtain units from here to ensure the static initializers have registered any custom units.
	 */
	public static <Q extends Quantity> Unit<Q> valueOf(String unit) {
		return (Unit<Q>) Unit.valueOf(unit);
	}

	public static BigDecimal convert(double value, Unit<?> unit) {
		return round(unit.getStandardUnit().getConverterTo(unit).convert(value));
	}

	public static BigDecimal round(double value) {
		return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
	}
}
