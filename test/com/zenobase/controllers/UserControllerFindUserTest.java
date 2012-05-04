package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;
import com.google.common.collect.ImmutableList;

import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.models.UserList;

public class UserControllerFindUserTest extends UserListControllerTestSupport {

	@Test
	public void testFindExistingUser() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(user.getId(), 0, 1);
		assertThat(result).hasStatus(OK).hasContent(new UserInfo(user).toJson());
	}

	@Test
	public void testUserNotFound() {
		Result result = call(user.getId(), 0, 1);
		assertThat(result).hasStatus(OK).hasContent(user.asIdentity().toJson());
	}

	@Test
	public void testListUsers() {
		UserList list = new UserList(ImmutableList.<User>of(), 0);
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(users.find(0, 1)).thenReturn(list);
		Result result = call(null, 0, 1);
		assertThat(result).hasStatus(OK).hasContent(list.toJson());
	}

	@Test
	public void testUnauthorizedToListUsers() {
		Result result = call(null, 0, 1);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testForbiddenToListUsers() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		Result result = call(null, 0, 1);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testDownloadUserList() {
		UserList list = new UserList(ImmutableList.<User>of(), 0);
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(users.find(0, 1)).thenReturn(list);
		Result result = call(null, 0, Integer.MAX_VALUE);
		assertThat(result).hasStatus(OK).hasContentType("text/plain");
	}

	private static Result call(String id, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.UserListController.find(id, offset, limit));
	}
}
