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

public class AuthorizationListControllerFindAllTest extends AuthorizationListControllerTestSupport {

	@Test
	public void test() {
		PartialList<Authorization> list = DefaultPartialList.of();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(authorizations.find(new AuthorizationQuery().queryString("scope:*"), 0, 10)).thenReturn(list);
		Result result = call("scope:*", 0, 10);
		assertThat(result).hasStatus(OK).hasContent(AuthorizationList.toJson(list));
	}

	@Test
	public void testWithoutAuthorization() {
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testWithScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), "someScope"));
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testWithNormalUser() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String q, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.AuthorizationListController.findAll(q, offset, limit));
	}
}
