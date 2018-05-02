package com.zenobase.common;

import java.math.BigInteger;
import java.security.SecureRandom;

public class Generator {

	private static final SecureRandom random = new SecureRandom();

	private Generator() {
		throw new AssertionError();
	}

	public static String id() {
		return id(50, "%10s");
	}

	public static String longId() {
		return id(100, "%20s");
	}

	private static String id(int bits, String format) {
		return String.format(format, new BigInteger(bits, random).toString(32)).replace(' ', '0');
	}
}
