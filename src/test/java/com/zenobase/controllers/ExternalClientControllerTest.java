package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
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
import com.zenobase.models.Bucket;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.ExternalClientList;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.ExternalClientQuery;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.ExternalClientRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class ExternalClientControllerTest extends ControllerTestSupport {

	private final AuthorizationContext auth = mock(AuthorizationContext.class);
	private final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	private final ExternalClientRepository clients = mock(ExternalClientRepository.class);
	private final BucketRepository buckets = mock(BucketRepository.class);
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
				bind(BucketRepository.class).toInstance(buckets);
				bind(UserRepository.class).toInstance(users);
				bind(ExternalClientController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		ExternalClientController controller = injector.getInstance(ExternalClientController.class);
		builder.get("/users/{userId}/external-clients/", controller::list);
		builder.put("/users/{userId}/external-clients/{clientId}", controller::put);
		builder.delete("/users/{userId}/external-clients/{clientId}", controller::revoke);
	}

	@Test
	public void testListUnauthorized() {
		when(auth.current(any())).thenReturn(null);
		try (Http1ClientResponse result = client.get("/users/" + user.getId() + "/external-clients/").request()) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testListRejectsExternalToken() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity, clientIdentity, "external"));
		try (Http1ClientResponse result = client.get("/users/" + user.getId() + "/external-clients/").request()) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testListReturnsConnectedClients() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity));
		ExternalClient connected = client();
		connected.setReadableBuckets(List.of("b1", "b2"));
		when(clients.find(any(ExternalClientQuery.class), anyInt(), anyInt())).thenReturn(
			new ExternalClientList(new NodeList(List.of(connected.toJson()), 1))
		);

		try (Http1ClientResponse result = client.get("/users/" + user.getId() + "/external-clients/").request()) {
			ObjectNode body = result.entity().as(ObjectNode.class);
			assertThat(result).hasStatus(200);
			assertThat(body.get("total").asInt()).isEqualTo(1);
			assertThat(body.get("external_clients")).hasSize(1);
			assertThat(body.get("external_clients").get(0).get("client_id").asText()).isEqualTo("claude-desktop");
			assertThat(body.get("external_clients").get(0).get("readable_buckets")).hasSize(2);
		}
	}

	@Test
	public void testPutDispatchesUpdateCommand() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity));
		ExternalClient connected = client();
		connected.setReadableBuckets(List.of("b1", "b2"));
		ExternalClient updated = client();
		updated.setReadableBuckets(List.of("b1", "b3"));
		when(clients.find(userIdentity, clientIdentity)).thenReturn(connected).thenReturn(updated);
		when(buckets.find("b1")).thenReturn(ownedBucket("b1"));
		when(buckets.find("b3")).thenReturn(ownedBucket("b3"));

		ObjectNode body = Nodes.newObject();
		body.putArray("readable_buckets").add("b1").add("b3");
		try (
			Http1ClientResponse result = client
				.put("/users/" + user.getId() + "/external-clients/claude-desktop")
				.submit(body)
		) {
			ObjectNode response = result.entity().as(ObjectNode.class);
			assertThat(result).hasStatus(200);
			assertThat(response.get("readable_buckets")).hasSize(2);
		}
		verify(dispatcher).dispatch(any(UpdateExternalClientGrantsCommand.class));
	}

	@Test
	public void testPutRejectsBucketTheUserDoesNotOwn() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity));
		when(clients.find(userIdentity, clientIdentity)).thenReturn(client());
		when(buckets.find("b1")).thenReturn(ownedBucket("b1"));
		// b2 exists but is owned by someone else — must not be acceptable as a grant target.
		Bucket strangersBucket = new Bucket("b2");
		strangersBucket.addRole(new Identity("stranger"), Role.OWNER);
		when(buckets.find("b2")).thenReturn(strangersBucket);

		ObjectNode body = Nodes.newObject();
		body.putArray("readable_buckets").add("b1").add("b2");
		try (
			Http1ClientResponse result = client
				.put("/users/" + user.getId() + "/external-clients/claude-desktop")
				.submit(body)
		) {
			assertThat(result).hasStatus(400);
		}
		verify(dispatcher, never()).dispatch(any());
	}

	@Test
	public void testPutRejectsUnknownBucket() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity));
		when(clients.find(userIdentity, clientIdentity)).thenReturn(client());
		when(buckets.find("ghost")).thenReturn(null);

		ObjectNode body = Nodes.newObject();
		body.putArray("readable_buckets").add("ghost");
		try (
			Http1ClientResponse result = client
				.put("/users/" + user.getId() + "/external-clients/claude-desktop")
				.submit(body)
		) {
			assertThat(result).hasStatus(400);
		}
		verify(dispatcher, never()).dispatch(any());
	}

	@Test
	public void testPut404WhenClientUnknown() {
		when(auth.current(any())).thenReturn(new Authorization(userIdentity));
		when(clients.find(userIdentity, clientIdentity)).thenReturn(null);

		ObjectNode body = Nodes.newObject();
		body.putArray("readable_buckets").add("b1");
		try (
			Http1ClientResponse result = client
				.put("/users/" + user.getId() + "/external-clients/claude-desktop")
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
				.delete("/users/" + user.getId() + "/external-clients/claude-desktop")
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

		try (Http1ClientResponse result = client.get("/users/other/external-clients/").request()) {
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
			new ExternalClientList(new NodeList(List.of(), 0))
		);

		try (Http1ClientResponse result = client.get("/users/other/external-clients/").request()) {
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

	private Bucket ownedBucket(String id) {
		Bucket bucket = new Bucket(id);
		bucket.addRole(userIdentity, Role.OWNER);
		return bucket;
	}
}
