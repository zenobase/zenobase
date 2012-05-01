package com.zenobase.controllers;

import static com.zenobase.test.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.User;
import com.zenobase.services.UserManager;

public class PasswordResetControllerTest {

	private final SecurityContext auth = mock(SecurityContext.class);
	private final UserManager users = mock(UserManager.class);
	private final PasswordResetMailer mailer = mock(PasswordResetMailer.class);
	private final User user = new User(Generator.id(), "tester");

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(UserManager.class).toInstance(users);
				bind(PasswordResetMailer.class).toInstance(mailer);
				requestStaticInjection(PasswordResetController.class);
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
