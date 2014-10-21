package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.PartialList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.oauth.AuthorizationList;
import com.zenobase.services.AuthorizationQuery;

public class AuthorizationListControllerFindByUserTest extends AuthorizationListControllerTestSupport {

	@Test
	public void test() {
		PartialList<Authorization> list = DefaultPartialList.of(new Authorization(user.asIdentity()));
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(authorizations.find(new AuthorizationQuery().principalEqualTo(user.asIdentity()).clientNotNull(), 0, 10)).thenReturn(list);
		Result result = call(user.getId(), Boolean.TRUE, null, 0, 10);
		assertThat(result).hasStatus(OK).hasContent(AuthorizationList.toJson(list));
	}

	@Test
	public void testAsSuperuser() {
		Identity superuser = new Identity();
		PartialList<Authorization> list = DefaultPartialList.of(new Authorization(user.asIdentity()));
		when(auth.current()).thenReturn(new Authorization(superuser));
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(authorizations.find(new AuthorizationQuery().principalEqualTo(user.asIdentity()).queryString("scope:*"), 0, 10)).thenReturn(list);
		Result result = call(user.getId(), null, "scope:*", 0, 10);
		assertThat(result).hasStatus(OK).hasContent(AuthorizationList.toJson(list));
	}

	@Test
	public void testLimitTooLow() {
		Result result = call(user.getId(), null, null, 0, -1);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testLimitTooHigh() {
		Result result = call(user.getId(), null, null, 0, 1000);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooLow() {
		Result result = call(user.getId(), null, null, -1, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooHigh() {
		Result result = call(user.getId(), null, null, 10000, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testMissingAuthorization() {
		Result result = call(user.getId(), null, null, 0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), "someScope"));
		Result result = call(user.getId(), null, null, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testUserNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call("@none", null, null, 0, 10);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testNotOwner() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		Result result = call(user.getId(), null, null, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String userId, Boolean hasClient, String q, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.AuthorizationListController.findByUser(userId, hasClient, q, offset, limit));
	}
}
