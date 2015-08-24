package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.junit.Before;
import play.test.FakeApplication;

import com.zenobase.mail.VerificationMailer;
import com.zenobase.models.User;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.services.LocalBus;
import com.zenobase.services.PaymentGateway;
import com.zenobase.services.TaskRepository;
import com.zenobase.services.UserRepository;

public abstract class AccountControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final BucketRepository buckets = mock(BucketRepository.class);
	protected final TaskRepository tasks = mock(TaskRepository.class);
	protected final CredentialsRepository credentials = mock(CredentialsRepository.class);
	protected final AuthorizationRepository authorizations = mock(AuthorizationRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final VerificationMailer mailer = mock(VerificationMailer.class);
	protected final PaymentGateway payments = mock(PaymentGateway.class);
	protected final User user = new User("tester");
	protected final String password = "secret123";

	@Override
	protected FakeApplication provideFakeApplication() {
		return fakeApplication(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(BucketRepository.class).toInstance(buckets);
				bind(UserRepository.class).toInstance(users);
				bind(TaskRepository.class).toInstance(tasks);
				bind(CredentialsRepository.class).toInstance(credentials);
				bind(AuthorizationRepository.class).toInstance(authorizations);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(VerificationMailer.class).toInstance(mailer); // unused
				bind(PaymentGateway.class).toInstance(payments);
				bind(AccountController.class).in(Singleton.class);
			}
		});
	}

	@Before
	public void setUp() {
		user.setEmail("jdoe@zenobase.com");
		user.setPassword(password);
	}
}
