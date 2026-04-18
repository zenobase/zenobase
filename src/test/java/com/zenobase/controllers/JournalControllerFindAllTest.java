package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.zenobase.commands.Command;
import com.zenobase.commands.TestCommand;
import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.Generator;
import com.zenobase.common.PartialList;
import com.zenobase.models.CommandList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.CommandQuery;
import io.helidon.webclient.http1.Http1ClientResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

public class JournalControllerFindAllTest extends JournalControllerTestSupport {

	@Test
	public void test() {
		PartialList<Command> history = DefaultPartialList.of(
			List.of(new TestCommand(user.asIdentity(), "do it"), new TestCommand(user.asIdentity(), "do it again")),
			10
		);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(commands.size()).thenReturn(history.getTotal());
		when(commands.find(new CommandQuery().queryString("foo"), CommandQuery.DEFAULT_ORDER, 0, 2)).thenReturn(
			history
		);
		try (Http1ClientResponse result = call("foo", 0, 2)) {
			assertThat(result).hasStatus(200).hasContent(CommandList.toJson(history));
		}
	}

	@Test
	public void testNotAuthorized() {
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity(), Generator.id()));
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testNotSuperuser() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	private Http1ClientResponse call(String q, int offset, int limit) {
		var request = client
			.get("/journal/")
			.queryParam("offset", String.valueOf(offset))
			.queryParam("limit", String.valueOf(limit));
		if (q != null) {
			request = request.queryParam("q", q);
		}
		return request.request();
	}
}
