package com.zenobase.services;

import com.zenobase.models.Identity;
import com.zenobase.models.User;

public class UserLookup {

	private final UserRepository repository;

	public UserLookup(UserRepository repository) {
		this.repository = repository;
	}

	public Identity getIdentity(String userId) {
		if (isName(userId)) {
			User user = find(userId);
			return user != null ? user.asIdentity() : null;
		}
		return new Identity(userId);
	}

	public User getUser(String userId) {
		return isName(userId)
			? find(userId)
			: find(new Identity(userId));
	}

	private User find(String userId) {
		return repository.find(userId.substring(1));
	}

	private User find(Identity identity) {
		User user = repository.find(identity);
		if (user == null) {
			user = new User(identity.getId(), null);
		}
		return user;
	}

	private static boolean isName(String userId) {
		return userId.startsWith("@") && userId.length() > 1;
	}
}