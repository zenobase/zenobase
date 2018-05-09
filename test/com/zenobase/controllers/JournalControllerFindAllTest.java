package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.commands.Command;
import com.zenobase.commands.TestCommand;
import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.Generator;
import com.zenobase.common.PartialList;
import com.zenobase.models.CommandList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.CommandQuery;

public class JournalControllerFindAllTest extends JournalControllerTestSupport {

	@Test
	public void test() {
		PartialList<Command> history = DefaultPartialList.of(ImmutableList.of(
			new TestCommand(user.asIdentity(), "do it"),
			new TestCommand(user.asIdentity(), "do it again")), 10);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(commands.size()).thenReturn(history.getTotal());
		when(commands.find(new CommandQuery().queryString("foo"), CommandQuery.DEFAULT_ORDER, 0, 2)).thenReturn(history);
		Result result = call("foo", 0, 2);
		assertThat(result).hasStatus(OK).hasContent(CommandList.toJson(history));
	}

	@Test
	public void testNotAuthorized() {
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), Generator.id()));
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testNotSuperuser() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private Result call(String q, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.JournalController.findAll(q, offset, limit));
	}
}
