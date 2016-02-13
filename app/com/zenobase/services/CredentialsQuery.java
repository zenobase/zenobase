package com.zenobase.services;

import org.joda.time.DateTime;

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

	public CredentialsQuery createdBefore(DateTime time) {
		lessThan(Credentials.CREATED, time);
		return this;
	}

	public CredentialsQuery notAuthorized() {
		notNull(Credentials.AUTHORIZATION_URL);
		return this;
	}

	@Override
	public CredentialsQuery queryString(String query) {
		super.queryString(query, Credentials.ID.getName());
		return this;
	}
}
