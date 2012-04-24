package com.zenobase.controllers;

import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.codehaus.jackson.node.ObjectNode;
import com.google.common.base.Strings;

import com.zenobase.models.DomainNode;
import com.zenobase.schema.TokenField;

public class SignUpForm extends DomainNode {

	private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-z0-9]{4,16}");

	private static final TokenField USERNAME = new TokenField("username");
	private static final TokenField PASSWORD = new TokenField("password");
	private static final TokenField EMAIL = new TokenField("email");

	public SignUpForm(ObjectNode node) {
		super(node);
	}

	public String getUsername() {
		return getValue(USERNAME);
	}

	public String getPassword() {
		return getValue(PASSWORD);
	}

	public String getEmail() {
		return getValue(EMAIL);
	}

	public boolean valid() {
		return isValidUsername(getUsername()) &&
			isValidPassword(getPassword()) &&
			isValidEmail(getEmail());
	}

	public static boolean isValidUsername(String value) {
		return !Strings.isNullOrEmpty(value) &&
			USERNAME_PATTERN.matcher(value).matches();
	}

	public static boolean isValidPassword(String value) {
		return !Strings.isNullOrEmpty(value) &&
			value.length() >= 8;
	}

	public static boolean isValidEmail(String value) {
		try {
			InternetAddress.parse(value);
		} catch (AddressException e) {
			return false;
		}
		return true;
	}
}
