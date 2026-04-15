package com.zenobase.common;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.regex.Pattern;

public class Generator {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Pattern ID_PATTERN = Pattern.compile("[0-9a-v]{10}");

	private Generator() {
		throw new AssertionError();
	}

	public static String id() {
		return id(50, "%10s");
	}

	public static String longId() {
		return id(100, "%20s");
	}

	public static boolean isValidId(String id) {
		return ID_PATTERN.matcher(id).matches();
	}

	private static String id(int bits, String format) {
		return String.format(format, new BigInteger(bits, RANDOM).toString(32)).replace(' ', '0');
	}
}
