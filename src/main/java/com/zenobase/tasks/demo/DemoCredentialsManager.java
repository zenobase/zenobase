package com.zenobase.tasks.demo;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.CredentialsManager;

public class DemoCredentialsManager extends CredentialsManager {

	private static final String TYPE = "demo";

	public DemoCredentialsManager() {
		super(TYPE);
	}

	@Override
	public Credentials newCredentials(Identity principal) {
		return new Credentials(TYPE, principal);
	}
}
