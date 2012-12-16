package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;

import com.zenobase.commands.DeleteAuthorizationCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class AuthorizationControllerHttpDeleteTest extends AuthorizationControllerTestSupport {

	private Authorization authorization = new Authorization(user.asIdentity(), null, Generator.id());

	@Test
	public void test() {
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(authorizations.find(authorization.getId())).thenReturn(authorization.copy());
		when(dispatcher.dispatch(any(DeleteAuthorizationCommand.class))).thenReturn(commandId);
		Result result = call(authorization.getId());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
	}

	@Test
	public void testAsSuperuser() {
		Identity superuser = new Identity();
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(superuser));
		when(authorizations.find(authorization.getId())).thenReturn(authorization.copy());
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(dispatcher.dispatch(any(DeleteAuthorizationCommand.class))).thenReturn(commandId);
		Result result = call(authorization.getId());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
	}

	@Test
	public void testNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(authorization.getId());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUnauthorized() {
		when(authorizations.find(authorization.getId())).thenReturn(authorization);
		Result result = call(authorization.getId());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(authorizations.find(authorization.getId())).thenReturn(authorization);
		Result result = call(authorization.getId());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	private static Result call(String id) {
		return callAction(com.zenobase.controllers.routes.ref.AuthorizationController.delete(id));
	}
}
