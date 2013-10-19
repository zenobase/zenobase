package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.joda.time.Period;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.mvc.Result;
import com.google.common.collect.Lists;

import com.zenobase.commands.CompoundCommand;
import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.Generator;
import com.zenobase.common.PartialList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class AuthorizationListControllerHttpDeleteTest extends AuthorizationListControllerTestSupport {

	@Test
	public void testDelete() {
		ArgumentCaptor<CompoundCommand> arg = ArgumentCaptor.forClass(CompoundCommand.class);
		String commandId = Generator.id();
		Authorization a1 = new Authorization(new Identity());
		Authorization a2 = new Authorization(new Identity());
		PartialList<Authorization> list = DefaultPartialList.<Authorization>of(Lists.newArrayList(a1, a2), 10L);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(authorizations.find(any(Period.class), anyInt(), anyInt())).thenReturn(list);
		when(dispatcher.dispatch(arg.capture())).thenReturn(commandId);
		Result result = call();
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId);
		assertThat(arg.getValue().getCommands()).hasSize(list.size());
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
