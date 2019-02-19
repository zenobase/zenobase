package com.zenobase.services;


import com.google.common.primitives.Ints;

import com.zenobase.common.StringBloomFilter;

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
		StringBloomFilter filter = new StringBloomFilter(Ints.checkedCast(users.size()));
		users.find(user -> filter.put(user.getId()));
		authorizations.find(new AuthorizationQuery().clientIsNull(), authorization -> filter.put(authorization.getPrincipal().getId()));
		return filter;
	}
}
