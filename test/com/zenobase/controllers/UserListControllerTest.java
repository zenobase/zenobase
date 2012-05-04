package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.models.UserList;
import com.zenobase.services.UserRepository;

public class UserListControllerTest {

	private final SecurityContext auth = mock(SecurityContext.class);
	private final UserRepository users = mock(UserRepository.class);
	private final User user = new User(Generator.id(), "tester");

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(UserRepository.class).toInstance(users);
				requestStaticInjection(UserListController.class);
			}
		});
	}

	@Test
	public void testFindUserForIndividual() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(user.getId(), 0, 1);
		assertThat(result).hasStatus(OK).hasContent(new UserInfo(user).toJson());
	}

	@Test
	public void testFindUserNotFound() {
		Result result = call(user.getId(), 0, 1);
		assertThat(result).hasStatus(OK).hasContent(user.asIdentity().toJson());
	}

	@Test
	public void testFindUsersPaged() {
		UserList expected = new UserList(ImmutableList.<User>of(), 0);
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(users.find(0, 1)).thenReturn(expected);
		Result result = call(null, 0, 1);
		assertThat(result).hasStatus(OK).hasContent(expected.toJson());
	}

	@Test
	public void testFindUsersNotLoggedIn() {
		Result result = call(null, 0, 1);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testFindUsersForbidden() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		Result result = call(null, 0, 1);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testDownloadUsers() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		Result result = call(null, 0, Integer.MAX_VALUE);
		assertThat(result).hasStatus(OK).hasContentType("text/plain");
	}

	private static Result call(String id, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.UserListController.find(id, offset, limit));
	}
}
