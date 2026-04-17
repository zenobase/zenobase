package com.zenobase.auth.auth0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.zenobase.auth.auth0.Auth0TokenValidator.Auth0Claims;
import com.zenobase.commands.ChangeExternalIdCommand;
import com.zenobase.commands.ChangeUserEmailCommand;
import com.zenobase.commands.ChangeUserVerifiedCommand;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;

public class Auth0UserSynchronizerTest {

	private final UserRepository users = mock(UserRepository.class);
	private final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	private final Auth0UserSynchronizer synchronizer = new Auth0UserSynchronizer(users, dispatcher);

	private static Auth0Claims claims(String username, String email, boolean emailVerified, String sub) {
		return new Auth0Claims(sub, username, email, emailVerified);
	}

	@Test
	public void testCreatesNewUser() {
		Auth0Claims claims = claims("testuser", "user@example.com", true, "auth0|123");
		when(users.find("testuser")).thenReturn(null);
		when(users.isEmpty()).thenReturn(false);

		Identity identity = synchronizer.sync(claims);

		assertThat(identity).isNotNull();
		ArgumentCaptor<CreateUserCommand> captor = ArgumentCaptor.forClass(CreateUserCommand.class);
		verify(dispatcher).dispatch(captor.capture());
		User created = captor.getValue().getUser();
		assertThat(created.getName()).isEqualTo("testuser");
		assertThat(created.getId()).isNotBlank().isNotEqualTo("auth0|123");
		assertThat(created.getExternalId()).isEqualTo("auth0|123");
		assertThat(created.getEmail()).isEqualTo("user@example.com");
		assertThat(created.isVerified()).isTrue();
		assertThat(created.isSuperuser()).isFalse();
	}

	@Test
	public void testCreatesFirstUserAsSuperuser() {
		Auth0Claims claims = claims("testuser", "user@example.com", true, "auth0|123");
		when(users.find("testuser")).thenReturn(null);
		when(users.isEmpty()).thenReturn(true);

		synchronizer.sync(claims);

		ArgumentCaptor<CreateUserCommand> captor = ArgumentCaptor.forClass(CreateUserCommand.class);
		verify(dispatcher).dispatch(captor.capture());
		assertThat(captor.getValue().getUser().isSuperuser()).isTrue();
	}

	@Test
	public void testReturnsNullWhenUsernameMissing() {
		Auth0Claims claims = claims(null, "user@example.com", true, "auth0|123");

		assertThat(synchronizer.sync(claims)).isNull();
		verifyNoInteractions(dispatcher);
	}

	@Test
	public void testBindsExternalIdOnFirstLogin() {
		User user = new User("id-1", "testuser");
		user.setEmail("user@example.com");
		user.setVerified(true);
		Auth0Claims claims = claims("testuser", "user@example.com", true, "auth0|123");
		when(users.find("testuser")).thenReturn(user);

		Identity identity = synchronizer.sync(claims);

		assertThat(identity).isNotNull();
		assertThat(identity.id()).isEqualTo("id-1");
		verify(dispatcher).dispatch(any(ChangeExternalIdCommand.class));
	}

	@Test
	public void testRejectsMismatchedExternalId() {
		User user = new User("id-1", "testuser");
		user.setExternalId("auth0|123");
		user.setEmail("user@example.com");
		user.setVerified(true);
		Auth0Claims claims = claims("testuser", "user@example.com", true, "auth0|attacker");
		when(users.find("testuser")).thenReturn(user);

		assertThat(synchronizer.sync(claims)).isNull();
		verifyNoInteractions(dispatcher);
	}

	@Test
	public void testAllowsMatchingExternalId() {
		User user = new User("id-1", "testuser");
		user.setExternalId("auth0|123");
		user.setEmail("user@example.com");
		user.setVerified(true);
		Auth0Claims claims = claims("testuser", "user@example.com", true, "auth0|123");
		when(users.find("testuser")).thenReturn(user);

		Identity identity = synchronizer.sync(claims);

		assertThat(identity).isNotNull();
		verifyNoInteractions(dispatcher);
	}

	@Test
	public void testSyncsVerified() {
		User user = new User("id-1", "testuser");
		user.setExternalId("auth0|123");
		user.setEmail("user@example.com");
		user.setVerified(false);
		Auth0Claims claims = claims("testuser", "user@example.com", true, "auth0|123");
		when(users.find("testuser")).thenReturn(user);

		synchronizer.sync(claims);

		verify(dispatcher).dispatch(any(ChangeUserVerifiedCommand.class));
	}

	@Test
	public void testSyncsEmail() {
		User user = new User("id-1", "testuser");
		user.setExternalId("auth0|123");
		user.setEmail("old@example.com");
		user.setVerified(true);
		Auth0Claims claims = claims("testuser", "new@example.com", true, "auth0|123");
		when(users.find("testuser")).thenReturn(user);

		synchronizer.sync(claims);

		verify(dispatcher).dispatch(any(ChangeUserEmailCommand.class));
	}

	@Test
	public void testDoesNotSyncEmailWhenAuth0NotVerified() {
		User user = new User("id-1", "testuser");
		user.setExternalId("auth0|123");
		user.setEmail("old@example.com");
		user.setVerified(true);
		Auth0Claims claims = claims("testuser", "new@example.com", false, "auth0|123");
		when(users.find("testuser")).thenReturn(user);

		synchronizer.sync(claims);

		verify(dispatcher, never()).dispatch(any(ChangeUserEmailCommand.class));
	}

	@Test
	public void testCachesSyncedAttributes() {
		User user = new User("id-1", "testuser");
		user.setExternalId("auth0|123");
		user.setEmail("user@example.com");
		user.setVerified(false);
		Auth0Claims claims = claims("testuser", "user@example.com", true, "auth0|123");
		when(users.find("testuser")).thenReturn(user);

		synchronizer.sync(claims);
		synchronizer.sync(claims);
		synchronizer.sync(claims);

		verify(dispatcher, times(1)).dispatch(any(ChangeUserVerifiedCommand.class));
	}

	@Test
	public void testReturnsNullOnException() {
		Auth0Claims claims = claims("testuser", "user@example.com", true, "auth0|123");
		when(users.find("testuser")).thenThrow(new RuntimeException("OpenSearch down"));

		assertThat(synchronizer.sync(claims)).isNull();
		verifyNoInteractions(dispatcher);
	}
}
