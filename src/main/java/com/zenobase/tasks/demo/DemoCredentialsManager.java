package com.zenobase.tasks.demo;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.CredentialsManager;
import jakarta.inject.Inject;

public class DemoCredentialsManager extends CredentialsManager {

	private static final String TYPE = "demo";

	@Inject
	public DemoCredentialsManager() {
		super(TYPE);
	}

	@Override
	public Credentials newCredentials(Identity principal) {
		return new Credentials(TYPE, principal);
	}
}
