package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.Test;

import com.zenobase.json.Nodes;
import com.zenobase.mail.PasswordResetMailer;
import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class PasswordResetControllerTest extends ControllerTestSupport {

	private final AuthorizationContext auth = mock(AuthorizationContext.class);
	private final UserRepository users = mock(UserRepository.class);
	private final PasswordResetMailer mailer = mock(PasswordResetMailer.class);
	private final User user = new User("tester");

	@Override
	protected void routing(HttpRouting.Builder builder) {
		var controller = new PasswordResetController(auth, users, mailer);
		builder.post("/reset", controller::requestReset);
	}

	@Test
	public void testSuccess() {
		user.setEmail("jdoe@zenobase.com");
		user.setVerified(true);
		when(users.find(user.getName())).thenReturn(user);
		ObjectNode body = Nodes.newObject();
		body.put(PasswordResetController.USERNAME.getName(), user.getName());
		body.put(User.EMAIL.getName(), user.getEmail());
		try (Http1ClientResponse result = call(body)) {
			assertThat(result).hasStatus(204).isEmpty();
			verify(mailer).send(user);
		}
	}

	@Test
	public void testUnverifiedUser() {
		user.setEmail("jdoe@zenobase.com");
		user.setVerified(false);
		when(users.find(user.getName())).thenReturn(user);
		ObjectNode body = Nodes.newObject();
		body.put(PasswordResetController.USERNAME.getName(), user.getName());
		try (Http1ClientResponse result = call(body)) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(mailer);
		}
	}

	@Test
	public void testEmailDoesntMatch() {
		user.setEmail("jdoe@zenobase.com");
		user.setVerified(true);
		when(users.find(user.getName())).thenReturn(user);
		ObjectNode body = Nodes.newObject();
		body.put(PasswordResetController.USERNAME.getName(), user.getName());
		try (Http1ClientResponse result = call(body)) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(mailer);
		}
	}

	@Test
	public void testMissingUsername() {
		user.setEmail("jdoe@zenobase.com");
		user.setVerified(true);
		when(users.find(user.getName())).thenReturn(user);
		ObjectNode body = Nodes.newObject();
		try (Http1ClientResponse result = call(body)) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(mailer);
		}
	}

	@Test
	public void testUserNotFound() {
		when(users.find(user.getName())).thenReturn(null);
		ObjectNode body = Nodes.newObject();
		body.put(PasswordResetController.USERNAME.getName(), user.getName());
		try (Http1ClientResponse result = call(body)) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(mailer);
		}
	}

	private Http1ClientResponse call(ObjectNode body) {
		return client.post("/reset").submit(body);
	}
}
