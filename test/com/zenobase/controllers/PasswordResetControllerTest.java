package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.junit.Test;
import play.mvc.Result;
import play.test.FakeApplication;

import com.zenobase.json.Nodes;
import com.zenobase.mail.PasswordResetMailer;
import com.zenobase.models.User;
import com.zenobase.services.Bus;
import com.zenobase.services.LocalBus;
import com.zenobase.services.UserRepository;

public class PasswordResetControllerTest extends ControllerTestSupport {

	private final AuthorizationContext auth = mock(AuthorizationContext.class);
	private final UserRepository users = mock(UserRepository.class);
	private final PasswordResetMailer mailer = mock(PasswordResetMailer.class);
	private final User user = new User("tester");

	@Override
	protected FakeApplication provideFakeApplication() {
		return fakeApplication(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(UserRepository.class).toInstance(users);
				bind(PasswordResetMailer.class).toInstance(mailer);
				bind(PasswordResetController.class).in(Singleton.class);
			}
		});
	}

	@Test
	public void testSuccess() {
		user.setEmail("jdoe@zenobase.com");
		user.setVerified(true);
		when(users.find(user.getName())).thenReturn(user);
		ObjectNode body = Nodes.newObject();
		body.put(PasswordResetController.USERNAME.getName(), user.getName());
		body.put(User.EMAIL.getName(), user.getEmail());
		Result result = call(body);
		assertThat(result).hasStatus(NO_CONTENT).isEmpty();
		verify(mailer).send(user);
	}

	@Test
	public void testUnverifiedUser() {
		user.setEmail("jdoe@zenobase.com");
		user.setVerified(false);
		when(users.find(user.getName())).thenReturn(user);
		ObjectNode body = Nodes.newObject();
		body.put(PasswordResetController.USERNAME.getName(), user.getName());
		Result result = call(body);
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(mailer);
	}

	@Test
	public void testEmailDoesntMatch() {
		user.setEmail("jdoe@zenobase.com");
		user.setVerified(true);
		when(users.find(user.getName())).thenReturn(user);
		ObjectNode body = Nodes.newObject();
		body.put(PasswordResetController.USERNAME.getName(), user.getName());
		Result result = call(body);
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(mailer);
	}

	@Test
	public void testMissingUsername() {
		user.setEmail("jdoe@zenobase.com");
		user.setVerified(true);
		when(users.find(user.getName())).thenReturn(user);
		ObjectNode body = Nodes.newObject();
		Result result = call(body);
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(mailer);
	}

	@Test
	public void testUserNotFound() {
		when(users.find(user.getName())).thenReturn(null);
		ObjectNode body = Nodes.newObject();
		body.put(PasswordResetController.USERNAME.getName(), user.getName());
		Result result = call(body);
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(mailer);
	}

	private static Result call(ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.PasswordResetController.requestReset(), fakeRequest().withJsonBody(body));
	}
}
