package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import org.junit.Test;

import com.zenobase.models.User;
import com.zenobase.models.UserProfile;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.Bus;
import com.zenobase.services.LocalBus;
import com.zenobase.services.UserRepository;

public class WhoControllerTest extends ControllerTestSupport {

	private final AuthorizationContext auth = mock(AuthorizationContext.class);
	private final UserRepository users = mock(UserRepository.class);
	private final User user = new User("tester");

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(UserRepository.class).toInstance(users);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		WhoController controller = injector.getInstance(WhoController.class);
		builder.get("/who", controller::who);
	}

	@Test
	public void testUnknown() {
		when(auth.current(any())).thenReturn(null);
		try (Http1ClientResponse result = call()) {
			assertThat(result).hasStatus(204).isEmpty();
		}
	}

	@Test
	public void testGuest() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(null);
		try (Http1ClientResponse result = call()) {
			assertThat(result).hasStatus(200).hasContent(user.asIdentity().toJson());
		}
	}

	@Test
	public void testUser() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		try (Http1ClientResponse result = call()) {
			assertThat(result).hasStatus(200).hasContent(new UserProfile(user).toJson());
		}
	}

	private Http1ClientResponse call() {
		return client.get("/who").request();
	}
}
