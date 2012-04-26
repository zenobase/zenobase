package com.zenobase.common;

import java.math.BigInteger;
import java.security.SecureRandom;

public class Generator {

	private static final SecureRandom rand = new SecureRandom();

	private Generator() {
		throw new AssertionError();
	}

	public static String id() {
		return String.format("%10s", new BigInteger(50, rand).toString(32)).replace(' ', '0');
	}
}
