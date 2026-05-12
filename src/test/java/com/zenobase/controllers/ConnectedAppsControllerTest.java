package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.zenobase.commands.CreateExternalBucketGrantCommand;
import com.zenobase.commands.DeleteExternalBucketGrantCommand;
import com.zenobase.json.NodeList;
import com.zenobase.models.ExternalBucketGrant;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.ExternalClientList;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.ExternalClientQuery;
import com.zenobase.repositories.ExternalBucketGrantRepository;
import com.zenobase.repositories.ExternalClientRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.Test;

public class ConnectedAppsControllerTest extends ControllerTestSupport {

	private final AuthorizationContext auth = mock(AuthorizationContext.class);
	private final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	private final ExternalClientRepository clients = mock(ExternalClientRepository.class);
	private final ExternalBucketGrantRepository grants = mock(ExternalBucketGrantRepository.class);
	private final UserRepository users = mock(UserRepository.class);

	private final User user = new User("tester");
	private final Identity userIdentity = user.asIdentity();
	private final Identity clientIdentity = new Identity("claude-desktop");

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(AuthorizationContext.class).toInstance(auth);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(ExternalClientRepository.class).toInstance(clients);
				bind(ExternalBucketGrantRepository.class).toInstance(grants);
				bind(UserRepository.class).toInstance(users);
				bind(ConnectedAppsController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		ConnectedAppsController controller = injector.getInstance(ConnectedAppsController.class);
		builder.get("/users/{userId}/connected-apps/", controller::list);
		builder.put("/users/{userId}/connected-apps/{clientId}/grants", controller::putGrants);
		builder.delete("/users/{userId}/connected-apps/{clientId}", controller::revoke);
	}

	@Test
	public void testListUnauthorized() {
		when(auth.current(any())).thenReturn(null);
		try (Http1ClientResponse result = client.get("/users/" + user.getId() + "/connected-apps/").request()) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testListRejectsExternalToken() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity, clientIdentity, "external"));
		try (Http1ClientResponse result = client.get("/users/" + user.getId() + "/connected-apps/").request()) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testListReturnsConnectedClients() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity));
		ExternalClient client1 = new ExternalClient(userIdentity, clientIdentity);
		when(clients.find(any(ExternalClientQuery.class), anyInt(), anyInt())).thenReturn(
			new ExternalClientList(new NodeList(java.util.List.of(client1.toJson()), 1))
		);
		when(grants.grantedBuckets(userIdentity, clientIdentity)).thenReturn(ImmutableSet.of("b1", "b2"));

		try (Http1ClientResponse result = client.get("/users/" + user.getId() + "/connected-apps/").request()) {
			ObjectNode body = result.entity().as(ObjectNode.class);
			assertThat(result).hasStatus(200);
			org.assertj.core.api.Assertions.assertThat(body.get("total").asInt()).isEqualTo(1);
			org.assertj.core.api.Assertions.assertThat(body.get("connected_apps")).hasSize(1);
			org.assertj.core.api.Assertions.assertThat(
				body.get("connected_apps").get(0).get("client_id").asText()
			).isEqualTo("claude-desktop");
			org.assertj.core.api.Assertions.assertThat(
				body.get("connected_apps").get(0).get("granted_bucket_ids")
			).hasSize(2);
		}
	}

	@Test
	public void testPutGrantsDiffsAgainstExisting() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity));
		when(grants.grantedBuckets(userIdentity, clientIdentity))
			.thenReturn(ImmutableSet.of("b1", "b2")) // existing
			.thenReturn(ImmutableSet.of("b1", "b3")); // post-update
		when(grants.find(userIdentity, clientIdentity, "b2")).thenReturn(
			new ExternalBucketGrant(userIdentity, clientIdentity, "b2", "read")
		);
		when(clients.find(userIdentity, clientIdentity)).thenReturn(new ExternalClient(userIdentity, clientIdentity));

		ObjectNode body = com.zenobase.json.Nodes.newObject();
		body.putArray("bucket_ids").add("b1").add("b3");
		try (
			Http1ClientResponse result = client
				.put("/users/" + user.getId() + "/connected-apps/claude-desktop/grants")
				.submit(body)
		) {
			assertThat(result).hasStatus(200);
		}

		// b3 was added, b2 was removed
		verify(dispatcher, times(1)).dispatch(any(CreateExternalBucketGrantCommand.class));
		verify(dispatcher, times(1)).dispatch(any(DeleteExternalBucketGrantCommand.class));
	}

	@Test
	public void testPutGrantsRejectsNonReadRights() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity));
		ObjectNode body = com.zenobase.json.Nodes.newObject();
		body.putArray("bucket_ids").add("b1");
		body.put("rights", "write");
		try (
			Http1ClientResponse result = client
				.put("/users/" + user.getId() + "/connected-apps/claude-desktop/grants")
				.submit(body)
		) {
			assertThat(result).hasStatus(400);
		}
		verify(dispatcher, never()).dispatch(any());
	}

	@Test
	public void testRevokeIssuesDeleteCommandsAndDropsClient() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity));
		when(grants.grantedBuckets(userIdentity, clientIdentity)).thenReturn(ImmutableSet.of("b1", "b2"));
		when(grants.find(userIdentity, clientIdentity, "b1")).thenReturn(
			new ExternalBucketGrant(userIdentity, clientIdentity, "b1", "read")
		);
		when(grants.find(userIdentity, clientIdentity, "b2")).thenReturn(
			new ExternalBucketGrant(userIdentity, clientIdentity, "b2", "read")
		);

		try (
			Http1ClientResponse result = client
				.delete("/users/" + user.getId() + "/connected-apps/claude-desktop")
				.request()
		) {
			assertThat(result).hasStatus(204);
		}

		verify(dispatcher, times(2)).dispatch(any(DeleteExternalBucketGrantCommand.class));
		verify(clients).delete(userIdentity, clientIdentity);
	}

	@Test
	public void testListForOtherUserForbiddenForNonSuperuser() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity));
		User other = new User("other");
		when(users.find("other")).thenReturn(other);

		try (Http1ClientResponse result = client.get("/users/other/connected-apps/").request()) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testSuperuserCanListAnyUser() {
		Identity superuser = new Identity("admin");
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.isSuperuser(superuser)).thenReturn(true);
		User other = new User("other");
		when(users.find("other")).thenReturn(other);
		when(clients.find(any(ExternalClientQuery.class), anyInt(), anyInt())).thenReturn(
			new ExternalClientList(new NodeList(java.util.List.of(), 0))
		);

		try (Http1ClientResponse result = client.get("/users/other/connected-apps/").request()) {
			assertThat(result).hasStatus(200);
		}
	}
}
