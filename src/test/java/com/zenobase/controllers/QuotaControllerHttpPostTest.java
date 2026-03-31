package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.zenobase.commands.SpendQuotaCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class QuotaControllerHttpPostTest extends QuotaControllerTestSupport {

	@Test
	public void test() {
		SpendQuotaCommand expected = new SpendQuotaCommand(new Identity(), -1000);
		ArgumentCaptor<SpendQuotaCommand> commandArg = ArgumentCaptor.forClass(SpendQuotaCommand.class);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(dispatcher.dispatch(commandArg.capture())).thenReturn(expected.getId());
		try (Http1ClientResponse result = call(expected.getPrincipal().id(), expected.getCost())) {
			assertThat(result).hasStatus(204).isEmpty();
			assertThat(commandArg.getValue().getPrincipal()).isEqualTo(expected.getPrincipal());
			assertThat(commandArg.getValue().getCost()).isEqualTo(expected.getCost());
		}
	}

	@Test
	public void testUnauthorized() {
		SpendQuotaCommand expected = new SpendQuotaCommand(new Identity(), -1000);
		try (Http1ClientResponse result = call(expected.getPrincipal().id(), expected.getCost())) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testForbidden() {
		SpendQuotaCommand expected = new SpendQuotaCommand(new Identity(), -1000);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(expected.getPrincipal().id(), expected.getCost())) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testBadRequest() {
		SpendQuotaCommand expected = new SpendQuotaCommand(new Identity(), -1000);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		try (Http1ClientResponse result = call(expected.getPrincipal().id(), 0)) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		try (Http1ClientResponse result = call("@nobody", 0)) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(dispatcher);
		}
	}

	private Http1ClientResponse call(String userId, int cost) {
		return client.post("/users/" + userId + "/quota").submit(Nodes.newObject("cost", cost));
	}
}
