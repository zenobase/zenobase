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

public class AuthorizationListControllerHttpGetTest extends AuthorizationListControllerTestSupport {

	@Test
	public void testFindByPrincipal() {
		PartialList<Authorization> list = DefaultPartialList.of();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(authorizations.find(Authorization.PRINCIPAL.getName(), user.asIdentity().toString(), false, 0, 10)).thenReturn(list);
		Result result = call(Authorization.PRINCIPAL.getName() + ":" + user.asIdentity(), false, 0, 10);
		assertThat(result).hasStatus(OK).hasContent(AuthorizationList.toJson(list));
	}

	@Test
	public void testFindByOtherPrincipal() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		Result result = call(Authorization.PRINCIPAL.getName() + ":" + user.asIdentity(), false, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testFindAllAsSuperuser() {
		PartialList<Authorization> list = DefaultPartialList.of();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(authorizations.find(0, 10)).thenReturn(list);
		Result result = call(null, false, 0, 10);
		assertThat(result).hasStatus(OK).hasContent(AuthorizationList.toJson(list));
	}

	@Test
	public void testFindAll() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(null, false, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testUnauthorized() {
		when(auth.current()).thenReturn(null);
		Result result = call(Authorization.PRINCIPAL.getName() + ":" + user.asIdentity(), false, 0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testBadLimit() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(Authorization.PRINCIPAL.getName() + ":" + user.asIdentity(), false, 0, Integer.MAX_VALUE);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	private static Result call(String query, boolean clientOnly, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.AuthorizationListController.find(query, clientOnly, offset, limit));
	}
}
