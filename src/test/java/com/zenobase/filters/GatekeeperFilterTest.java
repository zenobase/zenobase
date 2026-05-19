package com.zenobase.filters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.Bus;
import com.zenobase.testing.ResultAssert;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.Test;

public class GatekeeperFilterTest extends FilterTestSupport {

	private final Bus bus = mock(Bus.class);
	private final UserRepository users = mock(UserRepository.class);
	private final AuthorizationContext authContext = mock(AuthorizationContext.class);

	@Override
	protected void configureFilters(HttpRouting.Builder routing) {
		routing.addFilter(new GatekeeperFilter(bus, users, authContext));
	}

	@Test
	public void testGetInReadOnlyModeProceeds() {
		when(bus.isReadOnly()).thenReturn(true);
		when(authContext.current(any())).thenReturn(null);

		try (Http1ClientResponse r = client.get("/ping").request()) {
			ResultAssert.assertThat(r).hasStatus(200);
		}
	}

	@Test
	public void testPostInReadOnlyModeForbiddenForNonSuperuser() {
		when(bus.isReadOnly()).thenReturn(true);
		when(authContext.current(any())).thenReturn(null);

		try (Http1ClientResponse r = client.post("/ping").submit("{}")) {
			ResultAssert.assertThat(r).hasStatus(503);
		}
	}

	@Test
	public void testPostInReadOnlyModeAllowedForSuperuser() {
		Identity superuser = new Identity("admin-1");
		when(bus.isReadOnly()).thenReturn(true);
		when(authContext.current(any())).thenReturn(new Authorization(superuser));
		when(users.isSuperuser(superuser)).thenReturn(true);

		try (Http1ClientResponse r = client.post("/ping").submit("{}")) {
			ResultAssert.assertThat(r).hasStatus(200);
		}
	}

	@Test
	public void testPostWhenNotReadOnlyProceeds() {
		when(bus.isReadOnly()).thenReturn(false);
		when(authContext.current(any())).thenReturn(null);

		try (Http1ClientResponse r = client.post("/ping").submit("{}")) {
			ResultAssert.assertThat(r).hasStatus(200);
		}
	}
}
