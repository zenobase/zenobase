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

public class JournalControllerFindByUserTest extends JournalControllerTestSupport {

	@Test
	public void test() {
		PartialList<Command> history = DefaultPartialList.of(ImmutableList.<Command>of(
			new TestCommand(user.asIdentity(), "do it"),
			new TestCommand(user.asIdentity(), "do it again")), 10);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(commands.size()).thenReturn(history.getTotal());
		when(commands.find(Command.PRINCIPAL.getName(), user.getId(), 0, 2, true)).thenReturn(history);
		Result result = call(user.getId(), 0, 2);
		assertThat(result).hasStatus(OK).hasContent(CommandList.toJson(history));
	}

	@Test
	public void testSuperuser() {
		Identity superuser = new Identity();
		PartialList<Command> history = DefaultPartialList.of(ImmutableList.<Command>of(
			new TestCommand(user.asIdentity(), "do it"),
			new TestCommand(user.asIdentity(), "do it again")), 10);
		when(auth.current()).thenReturn(new Authorization(superuser));
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(commands.size()).thenReturn(history.getTotal());
		when(commands.find(Command.PRINCIPAL.getName(), user.getId(), 0, 2, true)).thenReturn(history);
		Result result = call(user.getId(), 0, 2);
		assertThat(result).hasStatus(OK).hasContent(CommandList.toJson(history));
	}

	@Test
	public void testNotAuthorized() {
		Result result = call(user.getId(), 0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), Generator.id()));
		Result result = call(user.getId(), 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testNotSuperuser() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		Result result = call(user.getId(), 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testUserNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call("@jdoe", 0, 10);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testNotOwner() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(users.find(user.getId())).thenReturn(user);
		Result result = call(user.getId(), 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private Result call(String userId, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.JournalController.findByUser(userId, offset, limit));
	}
}
