package com.zenobase.services;


import com.google.common.primitives.Ints;
import play.Logger;
import play.libs.Json;

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
		users.find(user -> {
			if (user.getId() != null) {
				filter.put(user.getId());
			} else {
				Logger.warn("Skipping user with no ID: {}", Json.toJson(user));
			}
		});
		authorizations.find(new AuthorizationQuery().clientIsNull(), authorization -> filter.put(authorization.getPrincipal().getId()));
		return filter;
	}
}
