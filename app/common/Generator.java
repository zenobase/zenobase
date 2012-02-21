package common;

import java.math.BigInteger;
import java.security.SecureRandom;

public class Generator {

	private static final SecureRandom rand = new SecureRandom();

	public static String id() {
		return String.format("%10s", new BigInteger(50, rand).toString(32)).replace(' ', '0');
	}
}
