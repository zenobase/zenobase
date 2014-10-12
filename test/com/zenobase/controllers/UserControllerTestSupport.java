package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import play.test.FakeApplication;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import com.zenobase.mail.VerificationMailer;
import com.zenobase.models.User;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.LocalBus;
import com.zenobase.services.PaymentGateway;
import com.zenobase.services.UserRepository;

public abstract class UserControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final AuthorizationRepository authorizations = mock(AuthorizationRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final VerificationMailer mailer = mock(VerificationMailer.class);
	protected final PaymentGateway payments = mock(PaymentGateway.class);
	protected final User user = new User("tester");

	@Override
	protected FakeApplication provideFakeApplication() {
		return fakeApplication(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(UserRepository.class).toInstance(users);
				bind(AuthorizationRepository.class).toInstance(authorizations);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(VerificationMailer.class).toInstance(mailer);
				bind(PaymentGateway.class).toInstance(payments);
				bind(UserController.class).in(Singleton.class);
			}
		});
	}
}
