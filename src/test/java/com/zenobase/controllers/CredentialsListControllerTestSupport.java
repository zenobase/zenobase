package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import io.helidon.webserver.http.HttpRouting;

import com.zenobase.models.User;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.services.UserRepository;
import com.zenobase.tasks.CredentialsManagerRegistry;

public abstract class CredentialsListControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final CredentialsManagerRegistry registry = mock(CredentialsManagerRegistry.class);
	protected final CredentialsRepository repository = mock(CredentialsRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final User user = new User("tester");

	@Override
	protected void routing(HttpRouting.Builder builder) {
		var controller = new CredentialsListController(auth, dispatcher, registry, repository, users);
		builder.get("/credentials/", controller::findAll);
		builder.get("/users/{userId}/credentials/", controller::findByUser);
		builder.post("/credentials/", controller::post);
	}
}
