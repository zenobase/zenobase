package com.zenobase.common;

import java.util.Scanner;

import org.joda.time.DurationFieldType;
import org.joda.time.Period;
import com.google.common.base.Preconditions;

public class PeriodFormat {

	private PeriodFormat() {

	}

	public static Period parse(String s) {
		if (isInteger(s)) {
			return Period.millis(Integer.parseInt(s));
		}
		Period duration = new Period();
		Scanner scanner = new Scanner(s);
		while (scanner.hasNext()) {
			String token = scanner.next();
			int i = indexOfFirstLetter(token);
			Preconditions.checkArgument(i > 0, String.format("Can't parse <%s> in <%s>", token, s));
			int amount = Integer.parseInt(token.substring(0, i));
			String unit = token.substring(i);
			duration = duration.withFieldAdded(valueOf(unit), amount);
		}
		scanner.close();
		return duration;
	}

	private static boolean isInteger(String s) {
		for (int i = 0; i < s.length(); ++i) {
			if (!Character.isDigit(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static int indexOfFirstLetter(String s) {
		for (int i = 0; i < s.length(); ++i) {
			if (Character.isLetter(s.charAt(i))) {
				return i;
			}
		}
		return -1;
	}

	private static DurationFieldType valueOf(String unit) {
		if ("y".equals(unit)) {
			return DurationFieldType.years();
		}
		if ("M".equals(unit)) {
			return DurationFieldType.months();
		}
		if ("w".equals(unit)) {
			return DurationFieldType.weeks();
		}
		if ("d".equals(unit)) {
			return DurationFieldType.days();
		}
		if ("h".equals(unit)) {
			return DurationFieldType.hours();
		}
		if ("min".equals(unit)) {
			return DurationFieldType.minutes();
		}
		if ("s".equals(unit)) {
			return DurationFieldType.seconds();
		}
		if ("ms".equals(unit)) {
			return DurationFieldType.millis();
		}
		throw new IllegalArgumentException(String.format("Can't handle unit <%s>", unit));
	}
}
