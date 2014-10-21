package com.zenobase.services;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;

public class CredentialsQuery extends QuerySupport {

	public CredentialsQuery principalEqualTo(Identity principal) {
		equalTo(Credentials.PRINCIPAL, principal.getId());
		return this;
	}

	public CredentialsQuery typeEqualTo(String type) {
		equalTo(Credentials.TYPE, type);
		return this;
	}

	@Override
	public CredentialsQuery queryString(String query) {
		super.queryString(query);
		return this;
	}
}
