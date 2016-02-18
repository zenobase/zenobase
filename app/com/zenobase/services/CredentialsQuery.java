package com.zenobase.services;

import org.joda.time.DateTime;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;

public class CredentialsQuery extends QuerySupport {

	private SearchOrder order = new SearchOrder(Credentials.CREATED.getName(), false);

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

	public CredentialsQuery order(String field, boolean asc) {
		this.order = new SearchOrder(field, asc);
		return this;
	}

	public SearchOrder order() {
		return order;
	}
}
