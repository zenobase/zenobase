package com.zenobase.controllers;

import static com.zenobase.test.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;

import com.zenobase.models.UserInfo;

public class UserControllerFindUserTest extends UserControllerTestSupport {

	@Test
	public void testFindExistingUser() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(user.getId(), 0, 1);
		assertThat(result).hasStatus(OK).hasContent(new UserInfo(user).toJson());
	}

	private static Result call(String id, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.UserController.find(id, 0, 1));

	}
}
