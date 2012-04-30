package com.zenobase.controllers;

import static com.zenobase.test.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.User;
import com.zenobase.services.UserManager;

public class PasswordResetControllerTest {

	private final SecurityContext auth = mock(SecurityContext.class);
	private final UserManager users = mock(UserManager.class);
	private final PasswordResetMailer mailer = mock(PasswordResetMailer.class);

	@Before
	public void setUp() {
		PasswordResetController.auth = auth;
		PasswordResetController.users = users;
		PasswordResetController.resetMailer = mailer;
	}

	@After
	public void tearDown() {
		PasswordResetController.auth = null;
		PasswordResetController.users = null;
		PasswordResetController.resetMailer = null;
	}

	@Test
	public void test() {
		User user = new User(Generator.id(), "tester");
		user.setVerified(true);
		user.setEmail("jdoe@zenobase.com");
		when(users.find(user.getName())).thenReturn(user);
		ObjectNode body = Nodes.newObject();
		body.put(PasswordResetController.USERNAME.getName(), user.getName());
		body.put(User.EMAIL.getName(), user.getEmail());
		Result result = callAction(com.zenobase.controllers.routes.ref.PasswordResetController.requestReset(), fakeRequest().withJsonBody(body));
		assertThat(result).hasStatus(NO_CONTENT).isEmpty();
		verify(mailer).send(user);
	}

	@Test
	public void testFailBecauseUnverified() {
		User user = new User(Generator.id(), "tester");
		when(users.find(user.getName())).thenReturn(user);
		ObjectNode body = Nodes.newObject();
		body.put(PasswordResetController.USERNAME.getName(), user.getName());
		Result result = callAction(com.zenobase.controllers.routes.ref.PasswordResetController.requestReset(), fakeRequest().withJsonBody(body));
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(mailer);
	}

	@Test
	public void testFailBecauseEmailDoesntMatch() {
		User user = new User(Generator.id(), "tester");
		user.setVerified(true);
		user.setEmail("jdoe@zenobase.com");
		when(users.find(user.getName())).thenReturn(user);
		ObjectNode body = Nodes.newObject();
		body.put(PasswordResetController.USERNAME.getName(), user.getName());
		Result result = callAction(com.zenobase.controllers.routes.ref.PasswordResetController.requestReset(), fakeRequest().withJsonBody(body));
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(mailer);
	}

	@Test
	public void testFailBecauseUsernameIsMissing() {
		User user = new User(Generator.id(), "tester");
		user.setVerified(true);
		user.setEmail("jdoe@zenobase.com");
		when(users.find(user.getName())).thenReturn(user);
		ObjectNode body = Nodes.newObject();
		Result result = callAction(com.zenobase.controllers.routes.ref.PasswordResetController.requestReset(), fakeRequest().withJsonBody(body));
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(mailer);
	}

	@Test
	public void testFailBecauseUserNotFound() {
		User user = new User(Generator.id(), "tester");
		user.setVerified(true);
		user.setEmail("jdoe@zenobase.com");
		when(users.find(user.getName())).thenReturn(user);
		ObjectNode body = Nodes.newObject();
		body.put(PasswordResetController.USERNAME.getName(), "me");
		Result result = callAction(com.zenobase.controllers.routes.ref.PasswordResetController.requestReset(), fakeRequest().withJsonBody(body));
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(mailer);
	}
}
