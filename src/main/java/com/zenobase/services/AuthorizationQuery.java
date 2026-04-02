package com.zenobase.services;

import org.joda.time.DateTime;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class AuthorizationQuery extends QuerySupport {

	public AuthorizationQuery principalEqualTo(Identity principal) {
		equalTo(Authorization.PRINCIPAL, principal.id());
		return this;
	}

	public AuthorizationQuery clientEqualTo(Identity client) {
		equalTo(Authorization.CLIENT, client.id());
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

	public AuthorizationQuery clientNotNull(@Nullable Boolean notNull) {
		if (Boolean.TRUE.equals(notNull)) {
			notNull(Authorization.CLIENT);
		} else if (Boolean.FALSE.equals(notNull)) {
			isNull(Authorization.CLIENT);
		}
		return this;
	}

	public AuthorizationQuery scopeEqualTo(@Nullable String scope) {
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
