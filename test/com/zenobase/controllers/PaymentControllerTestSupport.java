package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import java.math.BigDecimal;

import play.test.FakeApplication;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import com.zenobase.models.Payment;
import com.zenobase.models.User;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.LocalBus;
import com.zenobase.services.PaymentGateway;
import com.zenobase.services.UserRepository;

public abstract class PaymentControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final PaymentGateway payments = mock(PaymentGateway.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final User user = new User("jdoe");
	protected final Payment payment = new Payment(new BigDecimal("5.00"), "4111 1111 1111 1111", "100", "2050", "01");

	@Override
	protected FakeApplication provideFakeApplication() {
		return fakeApplication(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(PaymentGateway.class).toInstance(payments);
				bind(UserRepository.class).toInstance(users);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(PaymentController.class).in(Singleton.class);
			}
		});
	}
}
