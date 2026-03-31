package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import io.helidon.webserver.http.HttpRouting;

import com.zenobase.models.User;
import com.zenobase.services.Bus;
import com.zenobase.services.LocalBus;
import com.zenobase.services.UserRepository;

public abstract class StatusControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final Bus bus = mock(LocalBus.class);
	protected final User user = new User("tester");

	@Override
	protected void routing(HttpRouting.Builder builder) {
		var controller = new StatusController(auth, users, bus);
		builder.get("/status", controller::get);
		builder.post("/status", controller::post);
	}
}
