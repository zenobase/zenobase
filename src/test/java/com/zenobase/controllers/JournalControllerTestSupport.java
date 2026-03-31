package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import io.helidon.webserver.http.HttpRouting;

import com.zenobase.models.User;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.UserRepository;

public abstract class JournalControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final CommandRepository commands = mock(CommandRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final User user = new User("jdoe");

	@Override
	protected void routing(HttpRouting.Builder builder) {
		var controller = new JournalController(auth, dispatcher, commands, users);
		builder.get("/journal/", controller::findAll);
		builder.get("/users/{userId}/journal/", controller::findByUser);
		builder.post("/journal/", controller::post);
	}
}
