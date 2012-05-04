package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.common.Generator;
import com.zenobase.mail.VerificationMailer;
import com.zenobase.models.User;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

public abstract class AccountControllerTestSupport {

	protected final SecurityContext auth = mock(SecurityContext.class);
	protected final BucketRepository buckets = mock(BucketRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final VerificationMailer mailer = mock(VerificationMailer.class);
	protected final User user = new User(Generator.id(), "tester");
	protected final String password = "secret123";

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(BucketRepository.class).toInstance(buckets);
				bind(UserRepository.class).toInstance(users);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(VerificationMailer.class).toInstance(mailer); // unused
				requestStaticInjection(AccountController.class);
			}
		});
		user.setEmail("jdoe@zenobase.com");
		user.setPassword(password);
	}
}
