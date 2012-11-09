package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import com.zenobase.mail.VerificationMailer;
import com.zenobase.models.User;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

public abstract class UserControllerTestSupport extends ControllerTestSupport {

	protected final SecurityContext auth = mock(SecurityContext.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final VerificationMailer mailer = mock(VerificationMailer.class);
	protected final User user = new User("tester");

	@Before
	public void setUp() {
		start(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(UserRepository.class).toInstance(users);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(VerificationMailer.class).toInstance(mailer);
				bind(UserController.class).in(Singleton.class);
			}
		});
	}
}
