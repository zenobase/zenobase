package com.zenobase.auth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.repositories.UserRepository;
import jakarta.inject.Inject;
import java.time.Duration;

public class UserStateCache {

	public enum UserState {
		MISSING,
		SUSPENDED,
		ACTIVE,
	}

	private static final int CACHE_MAX_SIZE = 10_000;
	private static final Duration CACHE_TTL = Duration.ofSeconds(60);

	private final UserRepository users;
	private final Cache<Identity, UserState> cache = CacheBuilder.newBuilder()
		.maximumSize(CACHE_MAX_SIZE)
		.expireAfterWrite(CACHE_TTL)
		.build();

	@Inject
	public UserStateCache(UserRepository users) {
		this.users = users;
	}

	public UserState lookup(Identity principal) {
		UserState cached = cache.getIfPresent(principal);
		if (cached != null) {
			return cached;
		}
		User user = users.find(principal);
		UserState state =
			user == null ? UserState.MISSING : user.isSuspended() ? UserState.SUSPENDED : UserState.ACTIVE;
		cache.put(principal, state);
		return state;
	}

	public void invalidate(Identity principal) {
		cache.invalidate(principal);
	}
}
