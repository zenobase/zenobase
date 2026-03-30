package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import io.helidon.webserver.http.HttpRouting;

import com.zenobase.mail.EmailValidator;
import com.zenobase.mail.RegexEmailValidator;
import com.zenobase.mail.VerificationMailer;
import com.zenobase.models.User;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.LocalBus;
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
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(UserRepository.class).toInstance(users);
				bind(AuthorizationRepository.class).toInstance(authorizations);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(VerificationMailer.class).toInstance(mailer);
				bind(EmailValidator.class).to(RegexEmailValidator.class);
				bind(UserController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		UserController controller = injector.getInstance(UserController.class);
		builder.get("/users/{userId}", controller::get);
		builder.post("/users/{userId}", controller::update);
	}
}
