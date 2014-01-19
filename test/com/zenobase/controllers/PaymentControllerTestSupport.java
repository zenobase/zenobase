package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import com.zenobase.models.Card;
import com.zenobase.models.User;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.PaymentGateway;
import com.zenobase.services.UserRepository;

public abstract class PaymentControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final PaymentGateway payments = mock(PaymentGateway.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final User user = new User("jdoe");
	protected final Card card = new Card("4111 1111 1111 1111", "100", "2050", "01");

	@Before
	public void setUp() {
		start(new AbstractModule() {
			@Override
			protected void configure() {
				bind(AuthorizationContext.class).toInstance(auth);
				bind(PaymentGateway.class).toInstance(payments);
				bind(UserRepository.class).toInstance(users);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(PaymentController.class).in(Singleton.class);
			}
		});
	}
}
