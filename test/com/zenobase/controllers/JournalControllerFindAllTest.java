package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;
import com.google.common.collect.ImmutableList;

import com.zenobase.commands.Command;
import com.zenobase.commands.TestCommand;
import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.Generator;
import com.zenobase.common.PartialList;
import com.zenobase.models.CommandList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class JournalControllerFindAllTest extends JournalControllerTestSupport {

	@Test
	public void test() {
		PartialList<Command> history = DefaultPartialList.of(ImmutableList.<Command>of(
			new TestCommand(user.asIdentity(), "do it"),
			new TestCommand(user.asIdentity(), "do it again")), 10);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(commands.size()).thenReturn(history.getTotal());
		when(commands.find(0, 2, true)).thenReturn(history);
		Result result = call(0, 2);
		assertThat(result).hasStatus(OK).hasContent(CommandList.toJson(history));
	}

	@Test
	public void testNotAuthorized() {
		Result result = call(0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), Generator.id()));
		Result result = call(0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testNotSuperuser() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private Result call(int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.JournalController.findAll(offset, limit));
	}
}
