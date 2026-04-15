package com.zenobase.auth.auth0;

import java.time.Duration;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.auth.auth0.Auth0TokenValidator.Auth0Claims;
import com.zenobase.commands.ChangeExternalIdCommand;
import com.zenobase.commands.ChangeUserEmailCommand;
import com.zenobase.commands.ChangeUserVerifiedCommand;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.models.User;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

public class Auth0UserSynchronizer {

	private static final Logger logger = LoggerFactory.getLogger(Auth0UserSynchronizer.class);

	private static final int CACHE_MAX_SIZE = 10_000;
	private static final Duration CACHE_TTL = Duration.ofHours(1);

	private final UserRepository users;
	private final CommandDispatcher dispatcher;
	private final Cache<String, Boolean> synced = CacheBuilder.newBuilder()
			.maximumSize(CACHE_MAX_SIZE)
			.expireAfterWrite(CACHE_TTL)
			.build();

	@Inject
	public Auth0UserSynchronizer(UserRepository users, CommandDispatcher dispatcher) {
		this.users = users;
		this.dispatcher = dispatcher;
	}

	public void sync(Auth0Claims claims) {
		String id = claims.identity().id();
		if (synced.getIfPresent(id) != null) {
			return;
		}
		try {
			User user = users.find(claims.identity());
			if (user == null) {
				createUser(claims);
			} else {
				syncExternalId(user, claims);
				syncVerified(user, claims);
				syncEmail(user, claims);
			}
			synced.put(id, Boolean.TRUE);
		} catch (Exception e) {
			logger.warn("Failed to sync user {} from Auth0: {}", id, e.getMessage());
		}
	}

	private void createUser(Auth0Claims claims) {
		String id = claims.identity().id();
		String name = claims.username() != null ? claims.username() : id;
		var user = new User(id, name);
		if (claims.externalId() != null) {
			user.setExternalId(claims.externalId());
		}
		if (claims.email() != null) {
			user.setEmail(claims.email());
		}
		user.setVerified(claims.emailVerified());
		user.setSuperuser(users.isEmpty());
		dispatcher.dispatch(new CreateUserCommand(claims.identity(), user));
		logger.info("Created user {} from Auth0 (sub={})", id, claims.externalId());
	}

	private void syncExternalId(User user, Auth0Claims claims) {
		if (claims.externalId() != null && user.getExternalId() == null) {
			String name = user.getName();
			if (name != null) {
				dispatcher.dispatch(new ChangeExternalIdCommand(claims.identity(), name, claims.externalId()));
				logger.debug("Set external_id for user {} to {}", name, claims.externalId());
			}
		}
	}

	private void syncVerified(User user, Auth0Claims claims) {
		if (claims.emailVerified() && !user.isVerified()) {
			String name = user.getName();
			if (name != null) {
				dispatcher.dispatch(new ChangeUserVerifiedCommand(claims.identity(), name, true));
				logger.debug("Marked user {} as verified from Auth0", name);
			}
		}
	}

	private void syncEmail(User user, Auth0Claims claims) {
		String auth0Email = claims.email();
		if (auth0Email != null && !auth0Email.equals(user.getEmail())) {
			String name = user.getName();
			String currentEmail = user.getEmail();
			if (name != null && currentEmail != null) {
				dispatcher.dispatch(new ChangeUserEmailCommand(
						claims.identity(), name, currentEmail, auth0Email, user.isVerified(), claims.emailVerified()));
				logger.debug("Synced email for user {} from Auth0", name);
			}
		}
	}
}
