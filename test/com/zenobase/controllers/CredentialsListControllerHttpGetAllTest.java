package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.CredentialsQuery;
import com.zenobase.tasks.CredentialsList;

public class CredentialsListControllerHttpGetAllTest extends CredentialsListControllerTestSupport {

	@Test
	public void test() {
		CredentialsList list = new CredentialsList(DefaultPartialList.of());
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(repository.find(new CredentialsQuery().queryString("type:foo"), 0, 10)).thenReturn(list);
		Result result = call("type:foo", 0, 10);
		assertThat(result).hasStatus(OK).hasContent(CredentialsList.toJson(list));
	}

	@Test
	public void testLimitTooLow() {
		Result result = call(null, 0, -1);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testLimitTooHigh() {
		Result result = call(null, 0, 1000);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooLow() {
		Result result = call(null, -1, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooHigh() {
		Result result = call(null, 10000, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testNotAuthorized() {
		when(auth.current()).thenReturn(null);
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), "xyz"));
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testNotSuperuser() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String q, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.CredentialsListController.findAll(q, offset, limit));
	}
}
