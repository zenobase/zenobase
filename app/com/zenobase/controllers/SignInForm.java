package com.zenobase.controllers;

import org.codehaus.jackson.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.DomainNode;

public class SignInForm extends DomainNode {

	private static final TokenField USERNAME = new TokenField("username");
	private static final TokenField PASSWORD = new TokenField("password");
	private static final BooleanField REMEMBER = new BooleanField("remember");

	public SignInForm(ObjectNode node) {
		super(node);
	}

	SignInForm(String username, String password, boolean remember) {
		setValue(USERNAME, username);
		setValue(PASSWORD, password);
		setValue(REMEMBER, remember);
	}

	public String getUsername() {
		return getValue(USERNAME);
	}

	public String getPassword() {
		return getValue(PASSWORD);
	}

	public boolean isRemember() {
		return Objects.firstNonNull(getValue(REMEMBER), Boolean.FALSE);
	}

	public boolean valid() {
		return !Strings.isNullOrEmpty(getUsername()) &&
			!Strings.isNullOrEmpty(getPassword());
	}
}
