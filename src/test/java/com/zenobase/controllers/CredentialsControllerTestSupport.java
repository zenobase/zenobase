package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import io.helidon.webserver.http.HttpRouting;

import com.zenobase.models.Identity;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.services.UserRepository;
import com.zenobase.tasks.CredentialsManagerRegistry;

public abstract class CredentialsControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final CredentialsManagerRegistry registry = mock(CredentialsManagerRegistry.class);
	protected final CredentialsRepository repository = mock(CredentialsRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final Identity principal = new Identity();

	@Override
	protected void routing(HttpRouting.Builder builder) {
		var controller = new CredentialsController(auth, dispatcher, registry, repository, users);
		builder.get("/credentials/{credentialsId}", controller::get);
		builder.post("/credentials/{credentialsId}", controller::update);
		builder.delete("/credentials/{credentialsId}", controller::delete);
	}
}
