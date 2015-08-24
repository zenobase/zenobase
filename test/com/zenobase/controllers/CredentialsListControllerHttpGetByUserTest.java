package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.CredentialsQuery;
import com.zenobase.tasks.CredentialsList;

public class CredentialsListControllerHttpGetByUserTest extends CredentialsListControllerTestSupport {

	@Test
	public void test() {
		CredentialsList list = new CredentialsList(DefaultPartialList.<ObjectNode>of());
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(repository.find(new CredentialsQuery().principalEqualTo(user.asIdentity()), 0, 10)).thenReturn(list);
		Result result = call(user.getId(), null, 0, 10);
		assertThat(result).hasStatus(OK).hasContent(CredentialsList.toJson(list));
	}

	@Test
	public void testLimitTooLow() {
		Result result = call(user.getId(), null, 0, -1);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testLimitTooHigh() {
		Result result = call(user.getId(), null, 0, 1000);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooLow() {
		Result result = call(user.getId(), null, -1, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooHigh() {
		Result result = call(user.getId(), null, 10000, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testNotAuthorized() {
		Result result = call(user.getId(), null, 0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), Generator.id()));
		Result result = call(user.getId(), null, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testUserNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call("@jdoe", null, 0, 10);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testNotOwner() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getId(), null, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testSuperuser() {
		Identity superuser = new Identity();
		CredentialsList list = new CredentialsList(DefaultPartialList.<ObjectNode>of());
		when(auth.current()).thenReturn(new Authorization(superuser));
		when(users.find(user.getName())).thenReturn(user);
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(repository.find(new CredentialsQuery().principalEqualTo(user.asIdentity()).queryString("type:foo"), 0, 10)).thenReturn(list);
		Result result = call(user.getId(), "type:foo", 0, 10);
		assertThat(result).hasStatus(OK).hasContent(CredentialsList.toJson(list));
	}

	private static Result call(String userId, String q, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.CredentialsListController.findByUser(userId, q, offset, limit));
	}
}
