package com.zenobase.common;

import com.google.common.base.Preconditions;
import java.util.Scanner;
import org.joda.time.Duration;

public class DurationFormat {

	private DurationFormat() {}

	public static Duration parse(String s) {
		if (isLong(s)) {
			return Duration.millis(Long.parseLong(s));
		}
		var duration = new Duration(0);
		var scanner = new Scanner(s);
		while (scanner.hasNext()) {
			String token = scanner.next();
			int i = indexOfFirstLetter(token);
			Preconditions.checkArgument(i > 0, "Can't parse <%s> in <%s>", token, s);
			long amount = Long.parseLong(token.substring(0, i));
			String unit = token.substring(i);
			duration = duration.withDurationAdded(valueOf(amount, unit), 1);
		}
		scanner.close();
		return duration;
	}

	private static boolean isLong(String s) {
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

	private static Duration valueOf(long amount, String unit) {
		return switch (unit) {
			case "d" -> Duration.standardDays(amount);
			case "h" -> Duration.standardHours(amount);
			case "min" -> Duration.standardMinutes(amount);
			case "s" -> Duration.standardSeconds(amount);
			case null, default -> throw new IllegalArgumentException(String.format("Can't handle unit <%s>", unit));
		};
	}
}
