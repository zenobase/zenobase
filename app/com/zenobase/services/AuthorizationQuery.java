package com.zenobase.services;

import org.joda.time.DateTime;

import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class AuthorizationQuery extends QuerySupport {

	public AuthorizationQuery principalEqualTo(Identity principal) {
		equalTo(Authorization.PRINCIPAL, principal.getId());
		return this;
	}

	public AuthorizationQuery clientEqualTo(Identity client) {
		equalTo(Authorization.CLIENT, client.getId());
		return this;
	}

	public AuthorizationQuery clientIsNull() {
		isNull(Authorization.CLIENT);
		return this;
	}

	public AuthorizationQuery clientNotNull() {
		notNull(Authorization.CLIENT);
		return this;
	}

	public AuthorizationQuery clientNotNull(Boolean notNull) {
		if (notNull == Boolean.TRUE) {
			notNull(Authorization.CLIENT);
		} else if (notNull == Boolean.FALSE) {
			isNull(Authorization.CLIENT);
		}
		return this;
	}

	public AuthorizationQuery scopeEqualTo(String scope) {
		equalTo(Authorization.SCOPE, scope);
		return this;
	}

	public AuthorizationQuery createdBefore(DateTime time) {
		lessThan(Authorization.CREATED, time);
		return this;
	}

	public AuthorizationQuery queryString(String query) {
		super.queryString(query, Authorization.ID.getName());
		return this;
	}
}
