package com.zenobase.common;

import org.mindrot.jbcrypt.BCrypt;

public class BCryptUtils {

	public static String hashpw(String password) {
		return BCrypt.hashpw(password, BCrypt.gensalt()).replace('.', '-');
	}

	public static boolean checkpw(String password, String hash) {
		return BCrypt.checkpw(password, hash.replace('-', '.'));
	}
}
