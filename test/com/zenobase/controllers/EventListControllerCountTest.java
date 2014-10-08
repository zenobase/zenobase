package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;

public class EventListControllerCountTest extends EventListControllerTestSupport {

	@Before
	public void setUp() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
	}

	@Test
	public void testCountEvents() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(events.size()).thenReturn(42L);
		Result result = call(null);
		assertThat(result).hasStatus(OK).asObjectNode().path("total").isEqualTo(42);
	}

	@Test
	public void testCountUserEvents() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(events.size(user.asIdentity())).thenReturn(7L);
		Result result = call(user.getId());
		assertThat(result).hasStatus(OK).asObjectNode().path("total").isEqualTo(7);
	}

	@Test
	public void testCountUserNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(users.find("@nobody")).thenReturn(null);
		Result result = call("@nobody");
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testCountUnauthorized() {
		when(auth.current()).thenReturn(null);
		Result result = call(null);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testCountForbidden() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		Result result = call(null);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String userId) {
		return callAction(com.zenobase.controllers.routes.ref.EventListController.count(userId));
	}
}
