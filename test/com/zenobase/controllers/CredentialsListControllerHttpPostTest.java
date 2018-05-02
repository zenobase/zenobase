package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.junit.Test;
import play.mvc.Result;
import play.test.Helpers;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.CredentialsManager;

public class CredentialsListControllerHttpPostTest extends CredentialsListControllerTestSupport {

	private final String type = "foo";
	private final CreateCredentialsForm form = new CreateCredentialsForm(type);

	@Test
	public void test() {
		CredentialsManager manager = mock(CredentialsManager.class);
		Credentials credentials = new Credentials(type, user.asIdentity());
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(registry.find(type)).thenReturn(manager);
		when(manager.newCredentials(user.asIdentity())).thenReturn(credentials);
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		Result result = call(form);
		assertThat(result).hasStatus(CREATED);
		assertThat(Helpers.redirectLocation(result)).isEqualTo(com.zenobase.controllers.routes.CredentialsController.get(credentials.getId()).toString());
		assertThat(Helpers.header(COMMAND_ID, result)).isEqualTo(commandId);
	}

	@Test
	public void testNotAuthorized() {
		Result result = call(form);
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testInvalidBody() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(new CreateCredentialsForm(Nodes.newObject()));
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testInvalidType() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(new CreateCredentialsForm("bar"));
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testDuplicate() {
		CredentialsManager manager = mock(CredentialsManager.class);
		Credentials credentials = new Credentials(type, user.asIdentity());
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(registry.find(type)).thenReturn(manager);
		when(repository.find(user.asIdentity(), type)).thenReturn(credentials);
		Result result = call(form);
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	private static Result call(CreateCredentialsForm form) {
		return callAction(com.zenobase.controllers.routes.ref.CredentialsListController.post(), fakeRequest().withJsonBody(form.toJson()));
	}
}
