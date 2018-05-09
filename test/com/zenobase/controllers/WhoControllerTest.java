package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.junit.Test;
import play.mvc.Result;
import play.test.FakeApplication;

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
	protected FakeApplication provideFakeApplication() {
		return fakeApplication(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(UserRepository.class).toInstance(users);
				bind(WhoController.class).in(Singleton.class);
			}
		});
	}

	@Test
	public void testUnknown() {
		when(auth.current()).thenReturn(null);
		Result result = call();
		assertThat(result).hasStatus(NO_CONTENT).isEmpty();
	}

	@Test
	public void testGuest() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(null);
		Result result = call();
		assertThat(result).hasStatus(OK).hasContent(user.asIdentity().toJson());
	}

	@Test
	public void testUser() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call();
		assertThat(result).hasStatus(OK).hasContent(new UserProfile(user).toJson());
	}

	private static Result call() {
		return callAction(com.zenobase.controllers.routes.ref.WhoController.who(), fakeRequest());
	}
}
