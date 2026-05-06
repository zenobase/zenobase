package com.zenobase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.zenobase.auth.UserStateCache.UserState;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.repositories.UserRepository;
import org.junit.jupiter.api.Test;

public class UserStateCacheTest {

	private final UserRepository users = mock(UserRepository.class);
	private final UserStateCache cache = new UserStateCache(users);

	@Test
	public void testActiveUser() {
		User user = new User("id-1", "alice");
		Identity principal = user.asIdentity();
		when(users.find(principal)).thenReturn(user);

		assertThat(cache.lookup(principal)).isEqualTo(UserState.ACTIVE);
	}

	@Test
	public void testSuspendedUser() {
		User user = new User("id-1", "alice");
		user.setSuspended(true);
		Identity principal = user.asIdentity();
		when(users.find(principal)).thenReturn(user);

		assertThat(cache.lookup(principal)).isEqualTo(UserState.SUSPENDED);
	}

	@Test
	public void testMissingUser() {
		Identity principal = new Identity("id-missing");
		when(users.find(principal)).thenReturn(null);

		assertThat(cache.lookup(principal)).isEqualTo(UserState.MISSING);
	}

	@Test
	public void testCachesResult() {
		User user = new User("id-1", "alice");
		Identity principal = user.asIdentity();
		when(users.find(principal)).thenReturn(user);

		cache.lookup(principal);
		cache.lookup(principal);
		cache.lookup(principal);

		verify(users, times(1)).find(principal);
	}

	@Test
	public void testInvalidate() {
		User user = new User("id-1", "alice");
		Identity principal = user.asIdentity();
		when(users.find(principal)).thenReturn(user);

		assertThat(cache.lookup(principal)).isEqualTo(UserState.ACTIVE);

		user.setSuspended(true);
		assertThat(cache.lookup(principal)).isEqualTo(UserState.ACTIVE); // still cached

		cache.invalidate(principal);
		assertThat(cache.lookup(principal)).isEqualTo(UserState.SUSPENDED);
	}
}
