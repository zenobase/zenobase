package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.services.BucketManager;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.UserManager;

public abstract class AccountControllerTestSupport {

	protected final SecurityContext auth = mock(SecurityContext.class);
	protected final BucketManager buckets = mock(BucketManager.class);
	protected final UserManager users = mock(UserManager.class);
	protected final CommandQueue queue = mock(CommandQueue.class);
	protected final VerificationMailer mailer = mock(VerificationMailer.class);
	protected final User user = new User(Generator.id(), "tester");
	protected final String password = "secret123";

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(BucketManager.class).toInstance(buckets);
				bind(UserManager.class).toInstance(users);
				bind(CommandQueue.class).toInstance(queue);
				bind(VerificationMailer.class).toInstance(mailer); // unused
				requestStaticInjection(AccountController.class);
			}
		});
		user.setEmail("jdoe@zenobase.com");
		user.setPassword(password);
	}
}
