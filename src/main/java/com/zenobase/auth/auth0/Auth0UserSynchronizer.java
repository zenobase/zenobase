package com.zenobase.auth.auth0;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zenobase.auth.auth0.Auth0TokenValidator.Auth0Claims;
import com.zenobase.commands.ChangeExternalIdCommand;
import com.zenobase.commands.ChangeUserEmailCommand;
import com.zenobase.commands.ChangeUserVerifiedCommand;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import jakarta.inject.Inject;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Auth0UserSynchronizer {

	private static final Logger logger = LoggerFactory.getLogger(Auth0UserSynchronizer.class);

	private static final int CACHE_MAX_SIZE = 1_000;
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

	public @Nullable Identity sync(Auth0Claims claims) {
		String username = claims.username();
		if (username == null) {
			logger.warn("Auth0 JWT missing username claim");
			return null;
		}
		try {
			User user = users.find(username);
			if (user == null) {
				user = createUser(username, claims);
			} else if (!isExternalIdBindingValid(user, claims)) {
				logger.warn(
					"Rejecting Auth0 login for {}: subject {} does not match bound external_id {}",
					username,
					claims.externalId(),
					user.getExternalId()
				);
				return null;
			} else if (synced.getIfPresent(user.getId()) == null) {
				syncExternalId(user, claims);
				syncVerified(user, claims);
				syncEmail(user, claims);
			}
			synced.put(user.getId(), Boolean.TRUE);
			return user.asIdentity();
		} catch (Exception e) {
			logger.warn("Failed to sync user {} from Auth0: {}", username, e.getMessage());
			return null;
		}
	}

	private User createUser(String username, Auth0Claims claims) {
		var user = new User(Generator.id(), username);
		user.setExternalId(claims.externalId());
		if (claims.email() != null) {
			user.setEmail(claims.email());
		}
		user.setVerified(claims.emailVerified());
		user.setSuperuser(users.isEmpty());
		dispatcher.dispatch(new CreateUserCommand(user.asIdentity(), user));
		logger.info("Created user {} from Auth0 (sub={})", username, claims.externalId());
		return user;
	}

	private boolean isExternalIdBindingValid(User user, Auth0Claims claims) {
		String bound = user.getExternalId();
		return bound == null || bound.equals(claims.externalId());
	}

	private void syncExternalId(User user, Auth0Claims claims) {
		if (user.getExternalId() == null) {
			String name = user.getName();
			if (name != null) {
				dispatcher.dispatch(new ChangeExternalIdCommand(user.asIdentity(), name, claims.externalId()));
				logger.debug("Set external_id for user {} to {}", name, claims.externalId());
			}
		}
	}

	private void syncVerified(User user, Auth0Claims claims) {
		if (claims.emailVerified() && !user.isVerified()) {
			String name = user.getName();
			if (name != null) {
				dispatcher.dispatch(new ChangeUserVerifiedCommand(user.asIdentity(), name, true));
				logger.debug("Marked user {} as verified from Auth0", name);
			}
		}
	}

	private void syncEmail(User user, Auth0Claims claims) {
		if (!claims.emailVerified()) {
			return;
		}
		String auth0Email = claims.email();
		if (auth0Email != null && !auth0Email.equals(user.getEmail())) {
			String name = user.getName();
			String currentEmail = user.getEmail();
			if (name != null && currentEmail != null) {
				dispatcher.dispatch(
					new ChangeUserEmailCommand(
						user.asIdentity(),
						name,
						currentEmail,
						auth0Email,
						user.isVerified(),
						claims.emailVerified()
					)
				);
				logger.debug("Synced email for user {} from Auth0", name);
			}
		}
	}
}
