package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.PartialList;
import com.zenobase.jobs.Snapshot;
import com.zenobase.jobs.SnapshotManager;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.IndexManager;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.SnapshotList;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.snapshot.SnapshotInfo;

public class SnapshotControllerTest extends ControllerTestSupport {

	private final AuthorizationContext auth = mock(AuthorizationContext.class);
	private final UserRepository users = mock(UserRepository.class);
	private final SnapshotManager snapshots = mock(SnapshotManager.class);
	private final IndexManager indexManager = mock(IndexManager.class);
	private final User user = new User("tester");

	@Override
	protected Module module() {
		when(indexManager.getSnapshotManager()).thenReturn(snapshots);
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(AuthorizationContext.class).toInstance(auth);
				bind(UserRepository.class).toInstance(users);
				bind(IndexManager.class).toInstance(indexManager);
				bind(SnapshotController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		SnapshotController controller = injector.getInstance(SnapshotController.class);
		builder.get("/snapshots/", controller::findAll);
		builder.post("/snapshots/", controller::snapshot);
		builder.delete("/snapshots/{snapshotId}", controller::delete);
	}

	@Test
	public void testFindAll() {
		SnapshotInfo info = SnapshotInfo.of(b ->
			b.snapshot("2026-04-18").state("SUCCESS").startTimeInMillis(1000L).endTimeInMillis(61000L)
		);
		PartialList<Snapshot> expected = DefaultPartialList.of(List.of(new Snapshot(info)), 1);
		authenticatedAsSuperuser();
		when(snapshots.findAll(0, 10)).thenReturn(expected);
		try (Http1ClientResponse result = client.get("/snapshots/").request()) {
			assertThat(result).hasStatus(200).hasContent(SnapshotList.toJson(expected).toString());
		}
	}

	@Test
	public void testFindAllNotAuthenticated() {
		try (Http1ClientResponse result = client.get("/snapshots/").request()) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testFindAllNotSuperuser() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = client.get("/snapshots/").request()) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testFindAllScopedTokenForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity(), "read"));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		try (Http1ClientResponse result = client.get("/snapshots/").request()) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testSnapshot() {
		authenticatedAsSuperuser();
		try (Http1ClientResponse result = client.post("/snapshots/").request()) {
			assertThat(result).hasStatus(204);
		}
		verify(snapshots).snapshot();
	}

	@Test
	public void testSnapshotNotAuthenticated() {
		try (Http1ClientResponse result = client.post("/snapshots/").request()) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testSnapshotNotSuperuser() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = client.post("/snapshots/").request()) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testDelete() {
		authenticatedAsSuperuser();
		try (Http1ClientResponse result = client.delete("/snapshots/abc").request()) {
			assertThat(result).hasStatus(204);
		}
		verify(snapshots).delete("abc");
	}

	@Test
	public void testDeleteNotAuthenticated() {
		try (Http1ClientResponse result = client.delete("/snapshots/abc").request()) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testDeleteNotSuperuser() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = client.delete("/snapshots/abc").request()) {
			assertThat(result).hasStatus(403);
		}
	}

	private void authenticatedAsSuperuser() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
	}
}
