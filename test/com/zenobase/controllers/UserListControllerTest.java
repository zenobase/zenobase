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
import com.google.inject.Singleton;

import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.models.UserList;
import com.zenobase.services.UserRepository;

public class UserListControllerTest extends ControllerTestSupport {

	private final SecurityContext auth = mock(SecurityContext.class);
	private final UserRepository users = mock(UserRepository.class);
	private final User user = new User("tester");

	@Before
	public void setUp() {
		start(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(UserRepository.class).toInstance(users);
				bind(UserListController.class).in(Singleton.class);
			}
		});
	}

	@Test
	public void testFindUserForIndividual() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(user.getId(), 0, 1, false);
		assertThat(result).hasStatus(OK).hasContent(new UserInfo(user).toJson());
	}

	@Test
	public void testFindUserDetailForIndividual() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.asIdentity())).thenReturn(user);
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		Result result = call(user.getId(), 0, 1, true);
		assertThat(result).hasStatus(OK).hasContent(user.toJson());
	}

	@Test
	public void testFindUserNotFound() {
		Result result = call(user.getId(), 0, 1, false);
		assertThat(result).hasStatus(OK).hasContent(user.asIdentity().toJson());
	}

	@Test
	public void testFindUsersPaged() {
		UserList expected = new UserList(ImmutableList.<User>of(), 0);
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(users.find(0, 1)).thenReturn(expected);
		Result result = call(null, 0, 1, true);
		assertThat(result).hasStatus(OK).hasContent(expected.toJson());
	}

	@Test
	public void testFindUsersNotLoggedIn() {
		Result result = call(null, 0, 1, true);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testFindUsersForbidden() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		Result result = call(null, 0, 1, true);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testDownloadUsers() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		Result result = call(null, 0, Integer.MAX_VALUE, true);
		assertThat(result).hasStatus(OK).hasContentType("text/plain");
	}

	private static Result call(String id, int offset, int limit, boolean detail) {
		return callAction(com.zenobase.controllers.routes.ref.UserListController.find(id, offset, limit, detail));
	}
}
