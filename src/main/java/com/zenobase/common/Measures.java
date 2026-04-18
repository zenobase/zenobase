package com.zenobase.common;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Doubles;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;
import org.jspecify.annotations.Nullable;

public class Measures {

	private Measures() {}

	public static <Q extends Quantity> DecimalMeasure<Q> valueOf(String s) {
		return DecimalMeasure.valueOf(s);
	}

	public static <Q extends Quantity> DecimalMeasure<Q> valueOf(BigDecimal value, String unit) {
		Preconditions.checkNotNull(value);
		Preconditions.checkNotNull(unit);
		Unit<Q> u = Units.valueOf(unit);
		return valueOf(value, u);
	}

	public static <Q extends Quantity> DecimalMeasure<Q> valueOf(BigDecimal value, Unit<Q> unit) {
		Preconditions.checkNotNull(value);
		Preconditions.checkNotNull(unit);
		return DecimalMeasure.valueOf(value, unit);
	}

	@SuppressWarnings("unchecked")
	public static <Q extends Quantity> DecimalMeasure<Q> toStandard(DecimalMeasure<Q> measure) {
		return Units.isStandard(measure.getUnit())
			? measure
			: measure.to((Unit<Q>) measure.getUnit().getStandardUnit(), MathContext.DECIMAL32);
	}

	public static @Nullable BigDecimal convert(double value, Unit<?> unit) {
		return round(Units.isStandard(unit) ? value : unit.getStandardUnit().getConverterTo(unit).convert(value));
	}

	public static @Nullable BigDecimal round(double value) {
		return round(value, 2);
	}

	public static @Nullable BigDecimal round(double value, int scale) {
		return Doubles.isFinite(value) ? round(new BigDecimal(value), scale) : null;
	}

	public static @Nullable BigDecimal round(@Nullable BigDecimal value) {
		return round(value, 2);
	}

	public static @Nullable BigDecimal round(@Nullable BigDecimal value, int scale) {
		return value != null ? value.setScale(scale, RoundingMode.HALF_UP) : null;
	}

	public static <Q extends Quantity> DecimalMeasure<Q> round(DecimalMeasure<Q> value) {
		return Objects.requireNonNull(round(value, 2));
	}

	public static <Q extends Quantity> @Nullable DecimalMeasure<Q> round(@Nullable DecimalMeasure<Q> value, int scale) {
		return value != null ? new DecimalMeasure<>(round(value.getValue(), scale), value.getUnit()) : null;
	}
}
