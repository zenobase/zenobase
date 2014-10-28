package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.mvc.Result;

import com.zenobase.commands.SpendQuotaCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class QuotaControllerHttpPostTest extends QuotaControllerTestSupport {

	@Test
	public void test() {
		SpendQuotaCommand expected = new SpendQuotaCommand(new Identity(), -1000);
		ArgumentCaptor<SpendQuotaCommand> commandArg = ArgumentCaptor.forClass(SpendQuotaCommand.class);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(dispatcher.dispatch(commandArg.capture())).thenReturn(expected.getId());
		Result result = call(expected.getPrincipal().getId(), expected.getCost());
		assertThat(result).hasStatus(NO_CONTENT).isEmpty();
		assertThat(commandArg.getValue().getPrincipal()).isEqualTo(expected.getPrincipal());
		assertThat(commandArg.getValue().getCost()).isEqualTo(expected.getCost());
	}

	@Test
	public void testUnauthorized() {
		SpendQuotaCommand expected = new SpendQuotaCommand(new Identity(), -1000);
		Result result = call(expected.getPrincipal().getId(), expected.getCost());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testForbidden() {
		SpendQuotaCommand expected = new SpendQuotaCommand(new Identity(), -1000);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(expected.getPrincipal().getId(), expected.getCost());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testBadRequest() {
		SpendQuotaCommand expected = new SpendQuotaCommand(new Identity(), -1000);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		Result result = call(expected.getPrincipal().getId(), 0);
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		Result result = call("@nobody", 0);
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	private static Result call(String userId, int cost) {
		return callAction(com.zenobase.controllers.routes.ref.QuotaController.post(userId), fakeRequest().withJsonBody(Nodes.newObject("cost", cost)));
	}
}
