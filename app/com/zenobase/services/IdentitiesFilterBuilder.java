package com.zenobase.services;


import com.google.common.primitives.Ints;

import com.zenobase.common.Callback;
import com.zenobase.common.StringBloomFilter;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;

/**
 * Builds a probabilistic filter to check for identities that are neither signed up nor have an existing authorization.
 */
public class IdentitiesFilterBuilder {

	private final UserRepository users;
	private final AuthorizationRepository authorizations;

	public IdentitiesFilterBuilder(UserRepository users, AuthorizationRepository authorizations) {
		this.users = users;
		this.authorizations = authorizations;
	}

	public StringBloomFilter build() {
		final StringBloomFilter filter = new StringBloomFilter(Ints.checkedCast(users.size()));
		users.find(new Callback<User>() {
			@Override
			public void call(User user) {
				filter.put(user.getId());
			}
		});
		authorizations.find(new AuthorizationQuery().clientIsNull(), new Callback<Authorization>() {
			@Override
			public void call(Authorization authorization) {
				filter.put(authorization.getPrincipal().getId());
			}
		});
		return filter;
	}
}
