package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Test;

import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.models.UserProfile;
import com.zenobase.oauth.Authorization;

public class UserControllerHttpGetTest extends UserControllerTestSupport {

	@Test
	public void testSelf() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		try (Http1ClientResponse result = call(user.getId())) {
			assertThat(result).hasStatus(200).hasContent(new UserProfile(user).toJson());
		}
	}

	@Test
	public void testSelfByName() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.getName())).thenReturn(user);
		try (Http1ClientResponse result = call('@' + user.getName())) {
			assertThat(result).hasStatus(200).hasContent(new UserProfile(user).toJson());
		}
	}

	@Test
	public void testNameNotFound() {
		try (Http1ClientResponse result = call('@' + user.getName())) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testNotFound() {
		try (Http1ClientResponse result = call(user.getId())) {
			assertThat(result).hasStatus(200).hasContent(new UserInfo(new User(user.getId(), null)).toJson());
		}
	}

	@Test
	public void testNotSelf() {
		when(users.find(user.asIdentity())).thenReturn(user);
		try (Http1ClientResponse result = call(user.getId())) {
			assertThat(result).hasStatus(200).hasContent(new UserInfo(user).toJson());
		}
	}

	@Test
	public void testSelfButScoped() {
		when(users.find(user.asIdentity())).thenReturn(user);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity(), "someScope"));
		try (Http1ClientResponse result = call(user.getId())) {
			assertThat(result).hasStatus(200).hasContent(new UserInfo(user).toJson());
		}
	}

	@Test
	public void testSuperuser() {
		Identity superuser = new Identity();
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(users.isSuperuser(superuser)).thenReturn(true);
		try (Http1ClientResponse result = call(user.getId())) {
			assertThat(result).hasStatus(200).hasContent(new UserProfile(user).toJson());
		}
	}

	private Http1ClientResponse call(String userId) {
		return client.get("/users/" + userId).request();
	}
}
