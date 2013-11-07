package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.TaskList;

public class TaskListControllerHttpGetAllTest extends TaskListControllerTestSupport {

	@Test
	public void test() {
		TaskList list = new TaskList(DefaultPartialList.<ObjectNode>of());
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(tasks.find(0, 10)).thenReturn(list);
		Result result = findAll(0, 10);
		assertThat(result).hasStatus(OK).hasContent(TaskList.toJson(list));
	}

	@Test
	public void testLimitTooLow() {
		Result result = findAll(0, -1);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testLimitTooHigh() {
		Result result = findAll(0, 1000);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooLow() {
		Result result = findAll(-1, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooHigh() {
		Result result = findAll(10000, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testNotAuthorized() {
		Result result = findAll(0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), Generator.id()));
		Result result = findAll(0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testNotSuperuser() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = findAll(0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result findAll(int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.TaskListController.findAll(offset, limit));
	}
}
