package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import io.helidon.webserver.http.HttpRouting;

import com.zenobase.models.User;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.QuotaManager;
import com.zenobase.services.UserRepository;

abstract class QuotaControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final QuotaManager quotas = mock(QuotaManager.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final User user = new User("tester");

	@Override
	protected void routing(HttpRouting.Builder builder) {
		var controller = new QuotaController(auth, users, quotas, dispatcher);
		builder.get("/users/{userId}/quota", controller::get);
		builder.post("/users/{userId}/quota", controller::post);
	}
}
