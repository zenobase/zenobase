package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import io.helidon.webserver.http.HttpRouting;

import com.zenobase.mail.RegexEmailValidator;
import com.zenobase.mail.VerificationMailer;
import com.zenobase.models.User;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

public abstract class UserControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final AuthorizationRepository authorizations = mock(AuthorizationRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final VerificationMailer mailer = mock(VerificationMailer.class);
	protected final User user = newUser("tester");

	private static User newUser(String name) {
		User user = new User(name);
		user.setEmail(name + "@example.com");
		return user;
	}

	@Override
	protected void routing(HttpRouting.Builder builder) {
		var controller = new UserController(auth, users, authorizations, dispatcher, mailer, new RegexEmailValidator());
		builder.get("/users/{userId}", controller::get);
		builder.post("/users/{userId}", controller::update);
	}
}
