package com.zenobase.services;


import com.google.common.primitives.Ints;

import com.zenobase.common.StringBloomFilter;
import com.zenobase.common.StringFilter;
import com.zenobase.common.StringHashFilter;

/**
 * Builds a probabilistic filter to check for identities that are neither signed up nor have an existing authorization.
 */
public class IdentitiesFilterBuilder {

	private final UserRepository users;
	private final AuthorizationRepository authorizations;
	private boolean deterministic;

	public IdentitiesFilterBuilder(UserRepository users, AuthorizationRepository authorizations) {
		this.users = users;
		this.authorizations = authorizations;
	}

	public IdentitiesFilterBuilder deterministic(boolean deterministic) {
		this.deterministic = deterministic;
		return this;
	}

	public StringFilter build() {
		StringFilter filter = deterministic
			? new StringHashFilter()
			: new StringBloomFilter(Ints.checkedCast(users.size()));
		users.find(user -> filter.put(user.getId()));
		authorizations.find(new AuthorizationQuery().clientIsNull(), authorization -> filter.put(authorization.getPrincipal().getId()));
		return filter;
	}
}
