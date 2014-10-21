package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;
import play.test.FakeApplication;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.PartialList;
import com.zenobase.models.User;
import com.zenobase.models.UserList;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.Bus;
import com.zenobase.services.LocalBus;
import com.zenobase.services.UserQuery;
import com.zenobase.services.UserRepository;

public class UserListControllerTest extends ControllerTestSupport {

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
				bind(UserListController.class).in(Singleton.class);
			}
		});
	}

	@Test
	public void test() {
		PartialList<User> expected = DefaultPartialList.of();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(users.find(new UserQuery().queryString("verified:true"), 0, 1)).thenReturn(expected);
		Result result = call("verified:true", 0, 1);
		assertThat(result).hasStatus(OK).hasContent(UserList.toJson(expected));
	}

	@Test
	public void testNotAuthorized() {
		Result result = call(null, 0, 1);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testNotSuperuser() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(null, 0, 1);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testDownload() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		Result result = call(null, 0, Integer.MAX_VALUE);
		assertThat(result).hasStatus(OK).hasContentType("text/plain");
	}

	private static Result call(String q, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.UserListController.find(q, offset, limit));
	}
}
