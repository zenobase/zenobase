package com.zenobase.common;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;
import org.joda.time.Period;

public class StandardPeriod implements Comparable<StandardPeriod> {

	private static final DateTime EPOCH = new DateTime(0L);

	private static final Pattern PATTERN = Pattern.compile("([+-])(\\d+)([yMwdhms])");

	private static final ImmutableMap<Character, DurationFieldType> FIELDS = ImmutableMap.<
			Character,
			DurationFieldType
		>builder()
		.put('y', DurationFieldType.years())
		.put('M', DurationFieldType.months())
		.put('w', DurationFieldType.weeks())
		.put('d', DurationFieldType.days())
		.put('h', DurationFieldType.hours())
		.put('m', DurationFieldType.minutes())
		.put('s', DurationFieldType.seconds())
		.build();

	private final Period period;

	private StandardPeriod(Period period) {
		this.period = period;
	}

	public static StandardPeriod valueOf(Period period) {
		return new StandardPeriod(period);
	}

	public static StandardPeriod valueOf(String s) {
		var period = new Period();
		Matcher m = PATTERN.matcher(s);
		int offset = 0;
		while (m.find()) {
			check(offset == m.start(), s);
			int sign = "-".equals(m.group(1)) ? -1 : 1;
			int value = sign * Integer.parseInt(m.group(2));
			DurationFieldType unit = FIELDS.get(m.group(3).charAt(0));
			check(unit != null, s);
			period = period.withFieldAdded(unit, value);
			offset = m.end();
		}
		check(offset == s.length(), s);
		return valueOf(period);
	}

	private static void check(boolean condition, String s) {
		Preconditions.checkArgument(condition, "Can't parse period <%s>", s);
	}

	public Period toPeriod() {
		return period;
	}

	@Override
	public String toString() {
		var s = new StringBuilder();
		for (Map.Entry<Character, DurationFieldType> entry : FIELDS.entrySet()) {
			append(entry.getValue(), entry.getKey(), s);
		}
		return s.toString();
	}

	private void append(DurationFieldType field, char symbol, StringBuilder s) {
		int value = period.get(field);
		if (value != 0) {
			s.append(value > 0 ? '+' : '-').append(Math.abs(value)).append(symbol);
		}
	}

	@Override
	public int compareTo(StandardPeriod that) {
		return period.toDurationFrom(EPOCH).compareTo(that.toPeriod().toDurationFrom(EPOCH));
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof StandardPeriod p && period.equals(p.toPeriod());
	}

	@Override
	public int hashCode() {
		return period.hashCode();
	}

	/**
	 * Returns true if a string contains a field symbol (and therefore contains a potential period).
	 */
	public static boolean hasPeriod(String s) {
		for (int i = 0; i < s.length(); ++i) {
			if (FIELDS.containsKey(s.charAt(i))) {
				return true;
			}
		}
		return false;
	}
}
