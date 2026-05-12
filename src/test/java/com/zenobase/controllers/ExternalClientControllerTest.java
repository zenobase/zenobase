package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.zenobase.commands.UpdateExternalClientGrantsCommand;
import com.zenobase.json.NodeList;
import com.zenobase.json.Nodes;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.ExternalClientList;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.ExternalClientQuery;
import com.zenobase.repositories.ExternalClientRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class ExternalClientControllerTest extends ControllerTestSupport {

	private final AuthorizationContext auth = mock(AuthorizationContext.class);
	private final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	private final ExternalClientRepository clients = mock(ExternalClientRepository.class);
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
				bind(UserRepository.class).toInstance(users);
				bind(ExternalClientController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		ExternalClientController controller = injector.getInstance(ExternalClientController.class);
		builder.get("/users/{userId}/connected-apps/", controller::list);
		builder.put("/users/{userId}/connected-apps/{clientId}", controller::put);
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
		ExternalClient connected = client();
		connected.setReadableBuckets(java.util.List.of("b1", "b2"));
		when(clients.find(any(ExternalClientQuery.class), anyInt(), anyInt())).thenReturn(
			new ExternalClientList(new NodeList(java.util.List.of(connected.toJson()), 1))
		);

		try (Http1ClientResponse result = client.get("/users/" + user.getId() + "/connected-apps/").request()) {
			ObjectNode body = result.entity().as(ObjectNode.class);
			assertThat(result).hasStatus(200);
			org.assertj.core.api.Assertions.assertThat(body.get("total").asInt()).isEqualTo(1);
			org.assertj.core.api.Assertions.assertThat(body.get("connected_apps")).hasSize(1);
			org.assertj.core.api.Assertions.assertThat(
				body.get("connected_apps").get(0).get("client_id").asText()
			).isEqualTo("claude-desktop");
			org.assertj.core.api.Assertions.assertThat(
				body.get("connected_apps").get(0).get("readable_buckets")
			).hasSize(2);
		}
	}

	@Test
	public void testPutDispatchesUpdateCommand() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity));
		ExternalClient connected = client();
		connected.setReadableBuckets(java.util.List.of("b1", "b2"));
		ExternalClient updated = client();
		updated.setReadableBuckets(java.util.List.of("b1", "b3"));
		when(clients.find(userIdentity, clientIdentity)).thenReturn(connected).thenReturn(updated);

		ObjectNode body = Nodes.newObject();
		body.putArray("readable_buckets").add("b1").add("b3");
		try (
			Http1ClientResponse result = client
				.put("/users/" + user.getId() + "/connected-apps/claude-desktop")
				.submit(body)
		) {
			ObjectNode response = result.entity().as(ObjectNode.class);
			assertThat(result).hasStatus(200);
			org.assertj.core.api.Assertions.assertThat(response.get("readable_buckets")).hasSize(2);
		}
		verify(dispatcher).dispatch(any(UpdateExternalClientGrantsCommand.class));
	}

	@Test
	public void testPut404WhenClientUnknown() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity));
		when(clients.find(userIdentity, clientIdentity)).thenReturn(null);

		ObjectNode body = Nodes.newObject();
		body.putArray("readable_buckets").add("b1");
		try (
			Http1ClientResponse result = client
				.put("/users/" + user.getId() + "/connected-apps/claude-desktop")
				.submit(body)
		) {
			assertThat(result).hasStatus(404);
		}
		verify(dispatcher, never()).dispatch(any());
	}

	@Test
	public void testRevokeDispatchesEmptySnapshotAndDeletes() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity));

		try (
			Http1ClientResponse result = client
				.delete("/users/" + user.getId() + "/connected-apps/claude-desktop")
				.request()
		) {
			assertThat(result).hasStatus(204);
		}
		verify(dispatcher).dispatch(any(UpdateExternalClientGrantsCommand.class));
		verify(clients).delete(userIdentity, clientIdentity);
	}

	@Test
	public void testListForOtherUserForbiddenForNonSuperuser() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity));
		when(users.find("other")).thenReturn(new User("other"));

		try (Http1ClientResponse result = client.get("/users/other/connected-apps/").request()) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testSuperuserCanListAnyUser() {
		Identity superuser = new Identity("admin");
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(users.find("other")).thenReturn(new User("other"));
		when(clients.find(any(ExternalClientQuery.class), anyInt(), anyInt())).thenReturn(
			new ExternalClientList(new NodeList(java.util.List.of(), 0))
		);

		try (Http1ClientResponse result = client.get("/users/other/connected-apps/").request()) {
			assertThat(result).hasStatus(200);
		}
	}

	private ExternalClient client() {
		return new ExternalClient(
			userIdentity,
			clientIdentity,
			"Claude Desktop",
			new DateTime(2026, 5, 1, 0, 0, DateTimeZone.UTC)
		);
	}
}
