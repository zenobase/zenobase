package com.zenobase.auth.auth0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.zenobase.auth.auth0.Auth0TokenValidator.Auth0Claims;
import com.zenobase.commands.ChangeUserEmailCommand;
import com.zenobase.commands.ChangeUserVerifiedCommand;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

public class Auth0UserSynchronizerTest {

	private final UserRepository users = mock(UserRepository.class);
	private final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	private final Auth0UserSynchronizer synchronizer = new Auth0UserSynchronizer(users, dispatcher);

	@Test
	public void testCreatesNewUser() {
		Auth0Claims claims =
				new Auth0Claims(new Identity("auth0-123"), "testuser", "user@example.com", true, "auth0|123");
		when(users.find(claims.identity())).thenReturn(null);
		when(users.isEmpty()).thenReturn(false);

		synchronizer.sync(claims);

		ArgumentCaptor<CreateUserCommand> captor = ArgumentCaptor.forClass(CreateUserCommand.class);
		verify(dispatcher).dispatch(captor.capture());
		User created = captor.getValue().getUser();
		assertThat(created.getId()).isEqualTo("auth0-123");
		assertThat(created.getEmail()).isEqualTo("user@example.com");
		assertThat(created.isVerified()).isTrue();
		assertThat(created.isSuperuser()).isFalse();
	}

	@Test
	public void testCreatesFirstUserAsSuperuser() {
		Auth0Claims claims =
				new Auth0Claims(new Identity("auth0-123"), "testuser", "user@example.com", true, "auth0|123");
		when(users.find(claims.identity())).thenReturn(null);
		when(users.isEmpty()).thenReturn(true);

		synchronizer.sync(claims);

		ArgumentCaptor<CreateUserCommand> captor = ArgumentCaptor.forClass(CreateUserCommand.class);
		verify(dispatcher).dispatch(captor.capture());
		assertThat(captor.getValue().getUser().isSuperuser()).isTrue();
	}

	@Test
	public void testSyncsVerified() {
		User user = new User("auth0-123", "testuser");
		user.setEmail("user@example.com");
		user.setVerified(false);
		Auth0Claims claims =
				new Auth0Claims(new Identity("auth0-123"), "testuser", "user@example.com", true, "auth0|123");
		when(users.find(claims.identity())).thenReturn(user);

		synchronizer.sync(claims);

		verify(dispatcher).dispatch(any(ChangeUserVerifiedCommand.class));
	}

	@Test
	public void testDoesNotSyncVerifiedWhenAlreadyVerified() {
		User user = new User("auth0-123", "testuser");
		user.setEmail("user@example.com");
		user.setVerified(true);
		Auth0Claims claims =
				new Auth0Claims(new Identity("auth0-123"), "testuser", "user@example.com", true, "auth0|123");
		when(users.find(claims.identity())).thenReturn(user);

		synchronizer.sync(claims);

		verifyNoInteractions(dispatcher);
	}

	@Test
	public void testDoesNotSyncVerifiedWhenAuth0NotVerified() {
		User user = new User("auth0-123", "testuser");
		user.setEmail("user@example.com");
		user.setVerified(false);
		Auth0Claims claims =
				new Auth0Claims(new Identity("auth0-123"), "testuser", "user@example.com", false, "auth0|123");
		when(users.find(claims.identity())).thenReturn(user);

		synchronizer.sync(claims);

		verifyNoInteractions(dispatcher);
	}

	@Test
	public void testSyncsEmail() {
		User user = new User("auth0-123", "testuser");
		user.setEmail("old@example.com");
		user.setVerified(true);
		Auth0Claims claims =
				new Auth0Claims(new Identity("auth0-123"), "testuser", "new@example.com", true, "auth0|123");
		when(users.find(claims.identity())).thenReturn(user);

		synchronizer.sync(claims);

		verify(dispatcher).dispatch(any(ChangeUserEmailCommand.class));
	}

	@Test
	public void testDoesNotSyncEmailWhenUnchanged() {
		User user = new User("auth0-123", "testuser");
		user.setEmail("user@example.com");
		user.setVerified(true);
		Auth0Claims claims =
				new Auth0Claims(new Identity("auth0-123"), "testuser", "user@example.com", true, "auth0|123");
		when(users.find(claims.identity())).thenReturn(user);

		synchronizer.sync(claims);

		verifyNoInteractions(dispatcher);
	}

	@Test
	public void testDoesNotSyncEmailWhenNull() {
		User user = new User("auth0-123", "testuser");
		user.setEmail("user@example.com");
		user.setVerified(true);
		Auth0Claims claims = new Auth0Claims(new Identity("auth0-123"), "testuser", null, true, "auth0|123");
		when(users.find(claims.identity())).thenReturn(user);

		synchronizer.sync(claims);

		verifyNoInteractions(dispatcher);
	}

	@Test
	public void testCachesAfterFirstSync() {
		User user = new User("auth0-123", "testuser");
		user.setEmail("user@example.com");
		user.setVerified(true);
		Auth0Claims claims =
				new Auth0Claims(new Identity("auth0-123"), "testuser", "user@example.com", true, "auth0|123");
		when(users.find(claims.identity())).thenReturn(user);

		synchronizer.sync(claims);
		synchronizer.sync(claims);
		synchronizer.sync(claims);

		verify(users, times(1)).find(claims.identity());
	}

	@Test
	public void testHandlesExceptionGracefully() {
		Auth0Claims claims =
				new Auth0Claims(new Identity("auth0-123"), "testuser", "user@example.com", true, "auth0|123");
		when(users.find(claims.identity())).thenThrow(new RuntimeException("OpenSearch down"));

		// Should not throw
		synchronizer.sync(claims);

		verifyNoInteractions(dispatcher);
	}

	@Test
	public void testRetriesAfterException() {
		User user = new User("auth0-123", "testuser");
		user.setEmail("user@example.com");
		user.setVerified(true);
		Auth0Claims claims =
				new Auth0Claims(new Identity("auth0-123"), "testuser", "user@example.com", true, "auth0|123");
		when(users.find(claims.identity()))
				.thenThrow(new RuntimeException("temporary"))
				.thenReturn(user);

		synchronizer.sync(claims); // fails, not cached
		synchronizer.sync(claims); // retries, succeeds

		verify(users, times(2)).find(claims.identity());
	}
}
