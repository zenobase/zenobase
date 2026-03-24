package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Test;

import com.zenobase.commands.Command;
import com.zenobase.commands.TestCommand;
import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.Generator;
import com.zenobase.common.PartialList;
import com.zenobase.models.CommandList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.CommandQuery;

public class JournalControllerFindByUserTest extends JournalControllerTestSupport {

	@Test
	public void test() {
		PartialList<Command> history = DefaultPartialList.of(ImmutableList.of(
			new TestCommand(user.asIdentity(), "do it"),
			new TestCommand(user.asIdentity(), "do it again")), 10);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(commands.size()).thenReturn(history.getTotal());
		when(commands.find(new CommandQuery().principalEqualTo(user.asIdentity()), CommandQuery.DEFAULT_ORDER, 0, 2)).thenReturn(history);
		try (Http1ClientResponse result = call(user.getId(), 0, 2)) {
			assertThat(result).hasStatus(200).hasContent(CommandList.toJson(history));
		}
	}

	@Test
	public void testSuperuser() {
		Identity superuser = new Identity();
		PartialList<Command> history = DefaultPartialList.of(ImmutableList.of(
			new TestCommand(user.asIdentity(), "do it"),
			new TestCommand(user.asIdentity(), "do it again")), 10);
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(commands.size()).thenReturn(history.getTotal());
		when(commands.find(new CommandQuery().principalEqualTo(user.asIdentity()), CommandQuery.DEFAULT_ORDER, 0, 2)).thenReturn(history);
		try (Http1ClientResponse result = call(user.getId(), 0, 2)) {
			assertThat(result).hasStatus(200).hasContent(CommandList.toJson(history));
		}
	}

	@Test
	public void testNotAuthorized() {
		try (Http1ClientResponse result = call(user.getId(), 0, 10)) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity(), Generator.id()));
		try (Http1ClientResponse result = call(user.getId(), 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testNotSuperuser() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		try (Http1ClientResponse result = call(user.getId(), 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testUserNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call("@jdoe", 0, 10)) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testNotOwner() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(users.find(user.getId())).thenReturn(user);
		try (Http1ClientResponse result = call(user.getId(), 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	private Http1ClientResponse call(String userId, int offset, int limit) {
		return client.get("/users/" + userId + "/journal/")
			.queryParam("offset", String.valueOf(offset))
			.queryParam("limit", String.valueOf(limit))
			.request();
	}
}
