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
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskList;

public class TaskListControllerHttpGetByUserTest extends TaskListControllerTestSupport {

	@Test
	public void testWithUserName() {
		TaskList list = new TaskList(DefaultPartialList.<ObjectNode>of());
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.getName())).thenReturn(user);
		when(tasks.find(Task.PRINCIPAL.getName(), user.getId(), 0, 10)).thenReturn(list);
		Result result = findByUser(user.getName(), 0, 10);
		assertThat(result).hasStatus(OK).hasContent(TaskList.toJson(list));
	}

	@Test
	public void testWithUserId() {
		TaskList list = new TaskList(DefaultPartialList.<ObjectNode>of());
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(Task.PRINCIPAL.getName(), user.getId(), 0, 10)).thenReturn(list);
		Result result = findByUser('@' + user.getId(), 0, 10);
		assertThat(result).hasStatus(OK).hasContent(TaskList.toJson(list));
	}

	@Test
	public void testLimitTooLow() {
		Result result = findByUser(user.getName(), 0, -1);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testLimitTooHigh() {
		Result result = findByUser(user.getName(), 0, 1000);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooLow() {
		Result result = findByUser(user.getName(), -1, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooHigh() {
		Result result = findByUser(user.getName(), 10000, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testNotAuthorized() {
		Result result = findByUser(user.getName(), 0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), Generator.id()));
		Result result = findByUser(user.getName(), 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testUserNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = findByUser(Generator.id(), 0, 10);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testNotOwner() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(users.find(user.getName())).thenReturn(user);
		Result result = findByUser(user.getName(), 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testSuperuser() {
		Identity superuser = new Identity();
		TaskList list = new TaskList(DefaultPartialList.<ObjectNode>of());
		when(auth.current()).thenReturn(new Authorization(superuser));
		when(users.find(user.getName())).thenReturn(user);
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(tasks.find(Task.PRINCIPAL.getName(), user.getId(), 0, 10)).thenReturn(list);
		Result result = findByUser(user.getName(), 0, 10);
		assertThat(result).hasStatus(OK).hasContent(TaskList.toJson(list));
	}

	private static Result findByUser(String username, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.TaskListController.findByUser(username, offset, limit));
	}
}
