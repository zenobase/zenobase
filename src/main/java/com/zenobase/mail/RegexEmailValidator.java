package com.zenobase.mail;

import java.util.regex.Pattern;

public class RegexEmailValidator implements EmailValidator {

	private static final Pattern EMAIL_PATTERN = Pattern.compile(".+@.+\\..+");

	@Override
	public boolean isValid(String email) {
		return EMAIL_PATTERN.matcher(email).matches();
	}
}
