package com.zenobase.controllers;

import play.data.validation.Constraints.Required;

public class SignInForm {

	@Required
	public String username;

	@Required
	public String password;
	
	public boolean remember;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isRemember() {
		return remember;
	}

	public void setRemember(boolean remember) {
		this.remember = remember;
	}
}
