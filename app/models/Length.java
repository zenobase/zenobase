package models;

import java.math.BigDecimal;

public class Length implements Comparable<Length> {

	public enum Unit {
		m,
		km,
		ft,
		mi
	}
	private final BigDecimal value;
	private final Unit unit;

	private Length(BigDecimal value, Unit unit) {
		this.value = value;
		this.unit = unit;
	}

	public static Length valueOf(BigDecimal value, Unit unit) {
		return new Length(value, unit);
	}

	public BigDecimal getValue() {
		return value;
	}

	public Unit getUnit() {
		return unit;
	}

	@Override
	public int compareTo(Length that) {
		return value.compareTo(that.value);
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Length && equals((Length) that);
	}

	private boolean equals(Length that) {
		return value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
