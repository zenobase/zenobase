package com.zenobase.auth.auth0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.auth0.client.mgmt.ClientsClient;
import com.auth0.client.mgmt.ManagementApi;
import com.auth0.client.mgmt.UsersClient;
import com.auth0.client.mgmt.core.SyncPagingIterable;
import com.auth0.client.mgmt.types.AuthenticationMethodTypeEnum;
import com.auth0.client.mgmt.types.GetClientResponseContent;
import com.auth0.client.mgmt.types.GetUserAuthenticationMethodResponseContent;
import com.auth0.client.mgmt.types.ListUsersRequestParameters;
import com.auth0.client.mgmt.types.UpdateUserRequestContent;
import com.auth0.client.mgmt.types.UserAuthenticationMethod;
import com.auth0.client.mgmt.types.UserResponseSchema;
import com.auth0.client.mgmt.users.AuthenticationMethodsClient;
import com.zenobase.auth.Passkey;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class Auth0ManagementServiceTest {

	private final ManagementApi client = mock(ManagementApi.class);
	private final UsersClient usersClient = mock(UsersClient.class);
	private final ClientsClient clientsClient = mock(ClientsClient.class);
	private final AuthenticationMethodsClient methodsClient = mock(AuthenticationMethodsClient.class);
	private final Auth0ManagementService service = new Auth0ManagementService(client);

	private static User user(String name, @org.jspecify.annotations.Nullable String externalId) {
		User user = new User("id-1", name);
		user.setExternalId(externalId);
		return user;
	}

	private static <T> SyncPagingIterable<T> page(List<T> items) {
		return new SyncPagingIterable<>(false, items, null, () -> null);
	}

	@Nested
	class UpdateEmail {

		@Test
		public void updatesWhenExternalIdPresent() {
			when(client.users()).thenReturn(usersClient);

			service.updateEmail(user("alice", "auth0|123"), "new@example.com");

			verify(usersClient).update(eq("auth0|123"), any(UpdateUserRequestContent.class));
		}

		@Test
		public void skipsWhenExternalIdMissing() {
			service.updateEmail(user("alice", null), "new@example.com");

			verifyNoInteractions(client);
		}

		@Test
		public void swallowsExceptions() {
			when(client.users()).thenReturn(usersClient);
			when(usersClient.update(anyString(), any(UpdateUserRequestContent.class))).thenThrow(
				new RuntimeException("Auth0 down")
			);

			// Should not propagate — the local email change already succeeded.
			service.updateEmail(user("alice", "auth0|123"), "new@example.com");
		}
	}

	@Nested
	class DeleteUser {

		@Test
		public void deletesByExternalId() {
			when(client.users()).thenReturn(usersClient);

			service.deleteUser(user("alice", "auth0|123"));

			verify(usersClient).delete("auth0|123");
		}

		@Test
		public void fallsBackToUsernameLookupWhenExternalIdMissing() {
			when(client.users()).thenReturn(usersClient);
			UserResponseSchema found = UserResponseSchema.builder().userId("auth0|found").build();
			when(usersClient.list(any(ListUsersRequestParameters.class))).thenReturn(page(List.of(found)));

			service.deleteUser(user("alice", null));

			verify(usersClient).delete("auth0|found");
		}

		@Test
		public void doesNotDeleteWhenUsernameLookupFindsNothing() {
			when(client.users()).thenReturn(usersClient);
			when(usersClient.list(any(ListUsersRequestParameters.class))).thenReturn(page(List.of()));

			service.deleteUser(user("alice", null));

			verify(usersClient, never()).delete(anyString());
		}

		@Test
		public void refusesToDeleteWhenUsernameLookupIsAmbiguous() {
			when(client.users()).thenReturn(usersClient);
			UserResponseSchema a = UserResponseSchema.builder().userId("auth0|a").build();
			UserResponseSchema b = UserResponseSchema.builder().userId("auth0|b").build();
			when(usersClient.list(any(ListUsersRequestParameters.class))).thenReturn(page(List.of(a, b)));

			service.deleteUser(user("alice", null));

			verify(usersClient, never()).delete(anyString());
		}

		@Test
		public void swallowsExceptions() {
			when(client.users()).thenReturn(usersClient);
			doThrow(new RuntimeException("Auth0 down")).when(usersClient).delete(anyString());

			service.deleteUser(user("alice", "auth0|123"));
		}
	}

	@Nested
	class ListPasskeys {

		@Test
		public void returnsEmptyWhenExternalIdMissing() {
			assertThat(service.listPasskeys(user("alice", null))).isEmpty();
			verifyNoInteractions(client);
		}

		@Test
		public void filtersToPasskeysAndMapsFields() {
			when(client.users()).thenReturn(usersClient);
			when(usersClient.authenticationMethods()).thenReturn(methodsClient);

			UserAuthenticationMethod passkey = mock(UserAuthenticationMethod.class);
			when(passkey.getType()).thenReturn(AuthenticationMethodTypeEnum.PASSKEY);
			when(passkey.getId()).thenReturn("pk-1");
			when(passkey.getName()).thenReturn(Optional.of("My Laptop"));
			when(passkey.getCreatedAt()).thenReturn(OffsetDateTime.parse("2024-01-01T00:00:00Z"));
			when(passkey.getLastAuthAt()).thenReturn(Optional.of(OffsetDateTime.parse("2024-02-01T00:00:00Z")));
			when(passkey.getUserAgent()).thenReturn(Optional.of("Chrome"));

			UserAuthenticationMethod password = mock(UserAuthenticationMethod.class);
			when(password.getType()).thenReturn(AuthenticationMethodTypeEnum.PASSWORD);

			when(methodsClient.list("auth0|123")).thenReturn(page(List.of(passkey, password)));

			List<Passkey> passkeys = service.listPasskeys(user("alice", "auth0|123"));

			assertThat(passkeys).containsExactly(
				new Passkey("pk-1", "My Laptop", "2024-01-01T00:00Z", "2024-02-01T00:00Z", "Chrome")
			);
		}

		@Test
		public void mapsAbsentOptionalFieldsToNull() {
			when(client.users()).thenReturn(usersClient);
			when(usersClient.authenticationMethods()).thenReturn(methodsClient);

			UserAuthenticationMethod passkey = mock(UserAuthenticationMethod.class);
			when(passkey.getType()).thenReturn(AuthenticationMethodTypeEnum.PASSKEY);
			when(passkey.getId()).thenReturn("pk-1");
			when(passkey.getName()).thenReturn(Optional.empty());
			when(passkey.getCreatedAt()).thenReturn(OffsetDateTime.parse("2024-01-01T00:00:00Z"));
			when(passkey.getLastAuthAt()).thenReturn(Optional.empty());
			when(passkey.getUserAgent()).thenReturn(Optional.empty());
			when(methodsClient.list("auth0|123")).thenReturn(page(List.of(passkey)));

			assertThat(service.listPasskeys(user("alice", "auth0|123"))).containsExactly(
				new Passkey("pk-1", null, "2024-01-01T00:00Z", null, null)
			);
		}

		@Test
		public void returnsEmptyOnException() {
			when(client.users()).thenReturn(usersClient);
			when(usersClient.authenticationMethods()).thenReturn(methodsClient);
			when(methodsClient.list(anyString())).thenThrow(new RuntimeException("Auth0 down"));

			assertThat(service.listPasskeys(user("alice", "auth0|123"))).isEmpty();
		}
	}

	@Nested
	class DeletePasskey {

		@Test
		public void skipsWhenExternalIdMissing() {
			service.deletePasskey(user("alice", null), "pk-1");
			verifyNoInteractions(client);
		}

		@Test
		public void deletesWhenMethodIsPasskey() {
			when(client.users()).thenReturn(usersClient);
			when(usersClient.authenticationMethods()).thenReturn(methodsClient);
			GetUserAuthenticationMethodResponseContent method = mock(GetUserAuthenticationMethodResponseContent.class);
			when(method.getType()).thenReturn(AuthenticationMethodTypeEnum.PASSKEY);
			when(methodsClient.get("auth0|123", "pk-1")).thenReturn(method);

			service.deletePasskey(user("alice", "auth0|123"), "pk-1");

			verify(methodsClient).delete("auth0|123", "pk-1");
		}

		@Test
		public void rejectsWhenMethodIsNotPasskey() {
			when(client.users()).thenReturn(usersClient);
			when(usersClient.authenticationMethods()).thenReturn(methodsClient);
			GetUserAuthenticationMethodResponseContent method = mock(GetUserAuthenticationMethodResponseContent.class);
			when(method.getType()).thenReturn(AuthenticationMethodTypeEnum.PASSWORD);
			when(methodsClient.get("auth0|123", "pk-1")).thenReturn(method);

			assertThatThrownBy(() -> service.deletePasskey(user("alice", "auth0|123"), "pk-1")).isInstanceOf(
				IllegalArgumentException.class
			);
			verify(methodsClient, never()).delete(anyString(), anyString());
		}
	}

	@Nested
	class GetApplicationName {

		@Test
		public void returnsNameFromClient() {
			when(client.clients()).thenReturn(clientsClient);
			GetClientResponseContent response = GetClientResponseContent.builder().name("My App").build();
			when(clientsClient.get("client-1")).thenReturn(response);

			assertThat(service.getApplicationName(new Identity("client-1"))).isEqualTo("My App");
		}

		@Test
		public void returnsNullOnException() {
			when(client.clients()).thenReturn(clientsClient);
			when(clientsClient.get(anyString())).thenThrow(new RuntimeException("Auth0 down"));

			assertThat(service.getApplicationName(new Identity("client-1"))).isNull();
		}
	}

	@Nested
	class DeleteApplication {

		@Test
		public void deletesExternalApplication() {
			when(client.clients()).thenReturn(clientsClient);
			GetClientResponseContent response = GetClientResponseContent.builder().isFirstParty(false).build();
			when(clientsClient.get("client-1")).thenReturn(response);

			service.deleteApplication(new Identity("client-1"));

			verify(clientsClient).delete("client-1");
		}

		@Test
		public void refusesToDeleteFirstPartyApplication() {
			when(client.clients()).thenReturn(clientsClient);
			GetClientResponseContent response = GetClientResponseContent.builder().isFirstParty(true).build();
			when(clientsClient.get("client-1")).thenReturn(response);

			service.deleteApplication(new Identity("client-1"));

			verify(clientsClient, never()).delete(anyString());
		}

		@Test
		public void refusesWhenFirstPartyFlagAbsent() {
			when(client.clients()).thenReturn(clientsClient);
			GetClientResponseContent response = GetClientResponseContent.builder().build();
			when(clientsClient.get("client-1")).thenReturn(response);

			service.deleteApplication(new Identity("client-1"));

			verify(clientsClient, never()).delete(anyString());
		}

		@Test
		public void swallowsExceptionsAndDoesNotDelete() {
			when(client.clients()).thenReturn(clientsClient);
			when(clientsClient.get(anyString())).thenThrow(new RuntimeException("Auth0 down"));

			service.deleteApplication(new Identity("client-1"));

			verify(clientsClient, never()).delete(anyString());
		}
	}
}
