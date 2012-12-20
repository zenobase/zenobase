package com.zenobase.services;


import com.google.common.primitives.Ints;

import com.zenobase.common.Callback;
import com.zenobase.common.StringBloomFilter;
import com.zenobase.models.User;

/**
 * Builds a probabilistic filter to check for unregistered identities.
 */
public class IdentitiesFilterBuilder {

	private final UserRepository users;

	public IdentitiesFilterBuilder(UserRepository users) {
		this.users = users;
	}

	public StringBloomFilter build() {
		final StringBloomFilter filter = new StringBloomFilter(Ints.checkedCast(users.size()));
		users.find(new Callback<User>() {
			@Override
			public void call(User user) {
				filter.put(user.getId());
			}
		});
		return filter;
	}
}
