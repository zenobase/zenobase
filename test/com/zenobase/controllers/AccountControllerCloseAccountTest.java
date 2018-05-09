package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class AccountControllerCloseAccountTest extends AccountControllerTestSupport {

	@Test
	public void testCloseAccount() {
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.getName())).thenReturn(user);
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		Result result = call(user.getName());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
		verify(payments).cancel(user.getName());
	}

	@Test
	public void testCloseAccountNotSignedIn() {
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher, payments);
	}

	@Test
	public void testCloseAccountNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(user.getName());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher, payments);
	}

	@Test
	public void testCloseAccountForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher, payments);
	}

	@Test
	public void testCloseAccountSignedInAsSuperuser() {
		Identity superuser = new Identity();
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(superuser));
		when(users.find(user.getName())).thenReturn(user);
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		Result result = call(user.getName());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
		verify(payments).cancel(user.getName());
	}

	private Result call(String username) {
		return callAction(com.zenobase.controllers.routes.ref.AccountController.close('@' + username));
	}
}
