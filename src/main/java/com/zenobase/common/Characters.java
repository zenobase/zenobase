package com.zenobase.common;

public class Characters {

	public static boolean isDigits(String s) {
		for (int i = 0; i < s.length(); ++i) {
			if (!Character.isDigit(s.charAt(i))) {
				return false;
			}
		}
		return !s.isEmpty();
	}
}
