package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.Credentials;

public class CredentialsControllerHttpDeleteTest extends CredentialsControllerTestSupport {

	private final String type = "test";
	private final Credentials credentials = new Credentials(type, principal);

	@Test
	public void test() {
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(principal));
		when(repository.find(credentials.getId())).thenReturn(credentials.copy());
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		Result result = call(credentials.getId());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
	}

	@Test
	public void testUnauthorized() {
		Result result = call(credentials.getId());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testNotFound() {
		when(auth.current()).thenReturn(new Authorization(principal));
		Result result = call(credentials.getId());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(repository.find(credentials.getId())).thenReturn(credentials);
		Result result = call(credentials.getId());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testAsSuperuser() {
		Identity superuser = new Identity();
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(superuser));
		when(repository.find(credentials.getId())).thenReturn(credentials.copy());
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		Result result = call(credentials.getId());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
	}

	private static Result call(String credentialsId) {
		return callAction(com.zenobase.controllers.routes.ref.CredentialsController.delete(credentialsId));
	}
}
