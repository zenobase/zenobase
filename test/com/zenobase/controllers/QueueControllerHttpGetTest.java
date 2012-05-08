package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.commands.TestCommand;
import com.zenobase.models.CommandList;
import com.zenobase.models.Identity;

public class QueueControllerHttpGetTest extends QueueControllerTestSupport {

	@Test
	public void testGet() {
		CommandList history = new CommandList(Lists.<Command>newArrayList(new TestCommand(principal, "do it"), new TestCommand(principal, "do it again")), 10);
		when(auth.getPrincipal()).thenReturn(principal);
		when(users.isSuperuser(principal)).thenReturn(true);
		when(commands.size()).thenReturn(history.size());
		when(commands.findAll(0, 2, true)).thenReturn(history);
		Result result = call(0, 2);
		assertThat(result).hasStatus(OK).hasContent(history.toJson());
	}

	@Test
	public void testGetUnauthorized() {
		when(auth.getPrincipal()).thenReturn(null);
		Result result = call(0, 1);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testGetForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(users.isSuperuser(principal)).thenReturn(false);
		Result result = call(0, 1);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private Result call(int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.QueueController.get(offset, limit));
	}
}
