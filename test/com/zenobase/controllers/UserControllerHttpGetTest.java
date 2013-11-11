package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;

import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.models.UserProfile;
import com.zenobase.oauth.Authorization;

public class UserControllerHttpGetTest extends UserControllerTestSupport {

	@Test
	public void testSelfByName() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName());
		assertThat(result).hasStatus(OK).hasContent(new UserProfile(user).toJson());
	}

	@Test
	public void testSelfById() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call('@' + user.getId());
		assertThat(result).hasStatus(OK).hasContent(new UserProfile(user).toJson());
	}

	@Test
	public void testNameNotFound() {
		Result result = call(user.getName());
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testIdNotFound() {
		Result result = call('@' + user.getId());
		assertThat(result).hasStatus(OK).hasContent(new UserInfo(new User(user.getId(), null)).toJson());
	}

	@Test
	public void testNotSelf() {
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName());
		assertThat(result).hasStatus(OK).hasContent(new UserInfo(user).toJson());
	}

	@Test
	public void testSelfButScoped() {
		when(users.find(user.getName())).thenReturn(user);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), "someScope"));
		Result result = call(user.getName());
		assertThat(result).hasStatus(OK).hasContent(new UserInfo(user).toJson());
	}

	@Test
	public void testSuperuser() {
		Identity superuser = new Identity();
		when(auth.current()).thenReturn(new Authorization(superuser));
		when(users.find(user.getName())).thenReturn(user);
		when(users.isSuperuser(superuser)).thenReturn(true);
		Result result = call(user.getName());
		assertThat(result).hasStatus(OK).hasContent(new UserProfile(user).toJson());
	}

	private static Result call(String name) {
		return callAction(com.zenobase.controllers.routes.ref.UserController.get(name));
	}
}
