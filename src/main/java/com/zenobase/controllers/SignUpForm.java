package com.zenobase.controllers;

import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.DomainNode;
import com.zenobase.json.TokenField;
import com.zenobase.mail.EmailValidator;

public class SignUpForm extends DomainNode {

	private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-z0-9]{4,16}");

	private static final TokenField USERNAME = new TokenField("username");
	private static final TokenField PASSWORD = new TokenField("password");
	private static final TokenField EMAIL = new TokenField("email");

	public SignUpForm(ObjectNode node) {
		super(node);
	}

	SignUpForm(String username, String password, String email) {
		setValue(USERNAME, username);
		setValue(PASSWORD, password);
		setValue(EMAIL, email);
	}

	public @Nullable String getUsername() {
		return getValue(USERNAME);
	}

	public @Nullable String getPassword() {
		return getValue(PASSWORD);
	}

	public @Nullable String getEmail() {
		return getValue(EMAIL);
	}

	public boolean valid(EmailValidator emailValidator) {
		return isValidUsername(getUsername())
				&& isValidPassword(getPassword())
				&& emailValidator.isValid(Strings.nullToEmpty(getEmail()));
	}

	public static boolean isValidUsername(@Nullable String value) {
		return !Strings.isNullOrEmpty(value)
				&& !value.contains("zenobase")
				&& !value.contains("admin")
				&& !"guest".equalsIgnoreCase(value)
				&& USERNAME_PATTERN.matcher(value).matches();
	}

	public static boolean isValidPassword(@Nullable String value) {
		return !Strings.isNullOrEmpty(value) && value.length() >= 8;
	}
}
