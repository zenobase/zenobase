package com.zenobase.controllers;

import static com.zenobase.testing.CallbackAnswer.doCallback;
import static com.zenobase.testing.ResultAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.joda.time.Period;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.mvc.Result;

import com.zenobase.commands.CompoundCommand;
import com.zenobase.common.Callback;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class AuthorizationListControllerHttpDeleteTest extends AuthorizationListControllerTestSupport {

	@Test
	public void testDelete() {
		ArgumentCaptor<CompoundCommand> arg = ArgumentCaptor.forClass(CompoundCommand.class);
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(dispatcher.dispatch(arg.capture())).thenReturn(commandId);
		doCallback(new Authorization(new Identity())).when(authorizations).find(any(Period.class), any(Callback.class));
		Result result = call();
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId);
		assertThat(arg.getValue().getCommands()).hasSize(1);
	}

	@Test
	public void testDeleteNothingToDo() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		Result result = call();
		assertThat(result).hasStatus(NO_CONTENT);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUnauthorized() {
		when(auth.current()).thenReturn(null);
		Result result = call();
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testForbidden() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call();
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	private static Result call() {
		return callAction(com.zenobase.controllers.routes.ref.AuthorizationListController.delete());
	}
}
